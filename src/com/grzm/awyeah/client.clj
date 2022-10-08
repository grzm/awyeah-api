;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns ^:skip-wiki com.grzm.awyeah.client
  "Impl, don't call directly."
  (:require
   [clojure.core.async :as a]
   [com.grzm.awyeah.credentials :as credentials]
   [com.grzm.awyeah.endpoint :as endpoint]
   [com.grzm.awyeah.http :as http]
   [com.grzm.awyeah.interceptors :as interceptors]
   [com.grzm.awyeah.region :as region]
   [com.grzm.awyeah.util :as util]))

(set! *warn-on-reflection* true)

(defprotocol ClientSPI
  (-get-info [_] "Intended for internal use only"))

(defrecord Client [info]
  ClientSPI
  (-get-info [_] info))

(defn client [client-meta info]
  (with-meta (->Client info) @client-meta))

(defmulti build-http-request
  "AWS request -> HTTP request."
  (fn [service _op-map]
    (get-in service [:metadata :protocol])))

(defmulti parse-http-response
  "HTTP response -> AWS response"
  (fn [service _op-map _http-response]
    (get-in service [:metadata :protocol])))

(defmulti sign-http-request
  "Sign the HTTP request."
  (fn [service _endpoint _credentials _http-request]
    (get-in service [:metadata :signatureVersion])))

;; TODO convey throwable back from impl
(defn ^:private handle-http-response
  [service op-map http-response]
  (try
    (if (:cognitect.anomalies/category http-response)
      http-response
      (parse-http-response service op-map http-response))
    (catch Throwable t
      {:cognitect.anomalies/category :cognitect.anomalies/fault
       ::throwable t})))

(defn ^:private with-endpoint [req {:keys [protocol hostname port path]}]
  (cond-> (-> req
              (assoc-in [:headers "host"] hostname)
              (assoc :server-name hostname))
    protocol (assoc :scheme protocol)
    port (assoc :server-port port)
    path (assoc :uri path)))

(defn ^:private put-throwable [result-ch t response-meta op-map]
  (a/put! result-ch (with-meta
                      {:cognitect.anomalies/category :cognitect.anomalies/fault
                       ::throwable t}
                      (swap! response-meta
                             assoc :op-map op-map))))

(defn send-request
  "For internal use. Send the request to AWS and return a channel which delivers the response.

  Alpha. Subject to change."
  [client op-map]
  (let [{:keys [service http-client region-provider credentials-provider endpoint-provider]}
        (-get-info client)
        response-meta (atom {})
        region-ch (region/fetch-async region-provider)
        creds-ch (credentials/fetch-async credentials-provider)
        response-ch (a/chan 1)
        result-ch (a/promise-chan)]
    (a/go
      (let [region (a/<! region-ch)
            creds (a/<! creds-ch)
            endpoint (endpoint/fetch endpoint-provider region)]
        (cond
          (:cognitect.anomalies/category region)
          (a/>! result-ch region)
          (:cognitect.anomalies/category creds)
          (a/>! result-ch creds)
          (:cognitect.anomalies/category endpoint)
          (a/>! result-ch endpoint)
          :else
          (try
            (let [req (-> (build-http-request service op-map)
                          (with-endpoint endpoint)
                          (update :body util/->bbuf)
                          ((partial interceptors/modify-http-request service op-map)))
                  http-request (sign-http-request service endpoint
                                                  creds
                                                  req)]
              (swap! response-meta assoc :http-request http-request)
              (http/submit http-client http-request response-ch))
            (catch Throwable t
              (put-throwable result-ch t response-meta op-map))))))
    (a/go
      (try
        (let [response (a/<! response-ch)]
          (a/>! result-ch (with-meta
                            (handle-http-response service op-map response)
                            (swap! response-meta assoc
                                   :http-response (update response :body util/bbuf->input-stream)))))
        (catch Throwable t
          (put-throwable result-ch t response-meta op-map))))
    result-ch))
