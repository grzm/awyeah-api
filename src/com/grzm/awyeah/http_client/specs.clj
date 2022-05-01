;; Copyright (c) Michael Glaesemann
;; Heavily inspired by congitect.http-client, Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns com.grzm.awyeah.http-client.specs
  (:require
   [clojure.spec.alpha :as s])
  (:import
   (java.nio ByteBuffer)))

(defn- keyword-or-non-empty-string? [x]
  (or (keyword? x)
      (and (string? x) (not-empty x))))

(s/def :com.grzm.awyeah.http-client/server-name string?)
(s/def :com.grzm.awyeah.http-client/server-port int?)
(s/def :com.grzm.awyeah.http-client/uri string?)
(s/def :com.grzm.awyeah.http-client/request-method keyword?)
(s/def :com.grzm.awyeah.http-client/scheme keyword-or-non-empty-string?)
(s/def :com.grzm.awyeah.http-client/timeout-msec int?)
(s/def :com.grzm.awyeah.http-client/meta map?)
(s/def :com.grzm.awyeah.http-client/body (s/nilable #(instance? ByteBuffer %)))
(s/def :com.grzm.awyeah.http-client/query-string string?)
(s/def :com.grzm.awyeah.http-client/headers map?)

(s/def :com.grzm.awyeah.http-client/submit-request
  (s/keys :req-un [:com.grzm.awyeah.http-client/server-name
                   :com.grzm.awyeah.http-client/server-port
                   :com.grzm.awyeah.http-client/uri
                   :com.grzm.awyeah.http-client/request-method
                   :com.grzm.awyeah.http-client/scheme]
          :opt [:com.grzm.awyeah.http-client/timeout-msec
                :com.grzm.awyeah.http-client/meta]
          :opt-un [:com.grzm.awyeah.http-client/body
                   :com.grzm.awyeah.http-client/query-string
                   :com.grzm.awyeah.http-client/headers]))

(s/def :com.grzm.awyeah.http-client/status int?)

(s/def :com.grzm.awyeah.http-client/submit-http-response
  (s/keys :req-un [:com.grzm.awyeah.http-client/status]
          :opt [:com.grzm.awyeah.http-client/meta]
          :opt-un [:com.grzm.awyeah.http-client/body
                   :com.grzm.awyeah.http-client/headers]))

(s/def :com.grzm.awyeah.http-client/error keyword?)
(s/def :com.grzm.awyeah.http-client/throwable #(instance? Throwable %))

(s/def :com.grzm.awyeah.http-client/submit-error-response
  (s/keys :req [:com.grzm.awyeah.http-client/error]
          :opt [:com.grzm.awyeah.http-client/throwable
                :com.grzm.awyeah.http-client/meta]))

(s/def :com.grzm.awyeah.http-client/submit-response
  (s/or :http-response :com.grzm.awyeah.http-client/submit-http-response
        :error-response :com.grzm.awyeah.http-client/submit-error-response))
