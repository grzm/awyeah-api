;; Copyright (c) Michael Glaesemann
;; Heavily inspired by congitect.http-client, Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns com.grzm.awyeah.http-client
  (:require
   [clojure.core.async :refer [put!] :as a]
   [clojure.spec.alpha :as s]
   [com.grzm.awyeah.http-client.client :as client]
   [com.grzm.awyeah.http-client.specs])
  (:import
   (clojure.lang ExceptionInfo)
   (java.net URI)
   (java.net.http HttpClient
                  HttpClient$Redirect
                  HttpHeaders
                  HttpRequest
                  HttpRequest$Builder
                  HttpRequest$BodyPublishers
                  HttpResponse
                  HttpResponse$BodyHandlers)
   (java.nio ByteBuffer)
   (java.time Duration)
   (java.util.function Function)))

(set! *warn-on-reflection* true)

(defn submit
  "Submit an http request, channel will be filled with response. Returns ch.

  Request map:

  :server-name        string
  :server-port         integer
  :uri                string
  :query-string       string, optional
  :request-method     :get/:post/:put/:head
  :scheme             :http or :https
  :headers            map from downcased string to string
  :body               ByteBuffer, optional
  :com.grzm.awyeah.http-client/timeout-msec   opt, total request send/receive timeout
  :com.grzm.awyeah.http-client/meta           opt, data to be added to the response map

  content-type must be specified in the headers map
  content-length is derived from the ByteBuffer passed to body

  Response map:

  :status              integer HTTP status code
  :body                ByteBuffer, optional
  :header              map from downcased string to string
  :com.grzm.awyeah.http-client/meta           opt, data from the request

  On error, response map is per cognitect.anomalies"
  ([client request]
   (submit client request (a/chan 1)))
  ([client request ch]
   (s/assert ::submit-request request)
   (client/submit client request ch)))

(def method-string
  {:get "GET"
   :post "POST"
   :put "PUT"
   :head "HEAD"
   :delete "DELETE"
   :patch "PATCH"})

(defn byte-buffer->byte-array
  [^ByteBuffer bbuf]
  (.rewind bbuf)
  (let [arr (byte-array (.remaining bbuf))]
    (.get bbuf arr)
    arr))

(defn flatten-headers [headers]
  (->> headers
       (mapcat (fn [[nom val]]
                 (if (coll? val)
                   (map (fn [v] [(name nom) v]) val)
                   [[(name nom) val]])))))

;; "host" is a restricted header.
;; The host header is part of the AWS signed headers signature,
;; so it's included in the list of headers for request processing,
;; but we let the java.net.http HttpRequest assign the host header
;; from the URI rather than setting it directly.
(def restricted-headers #{"host"})

(defn add-headers
  [^HttpRequest$Builder builder headers]
  (doseq [[nom val] (->> (flatten-headers headers)
                         (remove (fn [[nom _]] (restricted-headers nom))))]
    (.header builder nom val))
  builder)

(defn map->http-request
  [{:keys [scheme server-name server-port uri query-string
           request-method headers body]
    :or {scheme "https"}
    :as m}]
  (let [uri (URI. (str (name scheme)
                       "://"
                       server-name
                       (some->> server-port (str ":"))
                       uri
                       (some->> query-string (str "?"))))
        method (method-string request-method)
        bp (if body
             (HttpRequest$BodyPublishers/ofByteArray (byte-buffer->byte-array body))
             (HttpRequest$BodyPublishers/noBody))
        builder (-> (HttpRequest/newBuilder uri)
                    (.method ^String method bp))]
    (when (seq headers)
      (add-headers builder headers))
    (when (::timeout-msec m)
      (.timeout builder (Duration/ofMillis (::timeout-msec m))))
    (.build builder)))

(defn error->anomaly [^Throwable t]
  {:cognitect.anomalies/category :cognitect.anomalies/fault
   :cognitect.anomalies/message (.getMessage t)
   ::throwable t})

(defn header-map [^HttpHeaders headers]
  (->> headers
       (.map)
       (map (fn [[k v]] [k (if (< 1 (count v))
                             (into [] v)
                             (first v))]))
       (into {})))

(defn response-body?
  [^HttpRequest http-request]
  ((complement #{"HEAD"}) (.method http-request)))

(defn response-map
  [^HttpRequest http-request ^HttpResponse http-response]
  (let [body (when (response-body? http-request)
               (.body http-response))]
    (cond-> {:status (.statusCode http-response)
             :headers (header-map (.headers http-response))}
      body (assoc :body (ByteBuffer/wrap body)))))

(defrecord Client
    [^HttpClient http-client pending-ops pending-ops-limit]
  client/Client
  (-submit [_ request ch]
    (if (< pending-ops-limit (swap! pending-ops inc))
      (do
        (put! ch (merge {:cognitect.anomalies/category :cognitect.anomalies/busy
                         :cognitect.anomalies/message (str "Ops limit reached: " pending-ops-limit)
                         :pending-ops-limit pending-ops-limit}
                        (select-keys request [::meta])))
        (swap! pending-ops dec))
      (try
        (let [http-request (map->http-request request)]
          (-> (.sendAsync http-client http-request (HttpResponse$BodyHandlers/ofByteArray))
              (.thenApply
                (reify Function
                  (apply [_ http-response]
                    (put! ch (merge (response-map http-request http-response)
                                    (select-keys request [::meta]))))))
              (.exceptionally
                (reify Function
                  (apply [_ e]
                    (let [cause (.getCause ^Exception e)
                          t (if (instance? ExceptionInfo cause) cause e)]
                      (put! ch (merge (error->anomaly t) (select-keys request [::meta]))))))))
          (swap! pending-ops dec))
        (catch Throwable t
          (put! ch (merge (error->anomaly t) (select-keys request [::meta])))
          (swap! pending-ops dec))))
    ch))

(defn create
  [{:keys [connect-timeout-msecs
           pending-ops-limit]
    :or {connect-timeout-msecs 5000
         pending-ops-limit 64}
    :as _config}]
  (let [http-client (.build (-> (HttpClient/newBuilder)
                                (.connectTimeout (Duration/ofMillis connect-timeout-msecs))
                                (.followRedirects HttpClient$Redirect/NORMAL)))]
    (->Client http-client (atom 0) pending-ops-limit)))

(defn stop
  "no-op. Implemented for compatibility"
  [^Client _client])
