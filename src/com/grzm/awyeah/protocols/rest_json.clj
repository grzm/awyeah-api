;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns ^:skip-wiki com.grzm.awyeah.protocols.rest-json
  "Impl, don't call directly."
  (:require
   [com.grzm.awyeah.protocols :as aws.protocols]
   [com.grzm.awyeah.protocols.rest :as rest]
   [com.grzm.awyeah.shape :as shape]
   [com.grzm.awyeah.util :as util]))

(set! *warn-on-reflection* true)

(defmulti serialize
  "json body args serializer passed to rest/build-http-request

  Obs: this fn doesn't use the first arg, but the one in rest-xml
  does, and this function gets invoked by rest/build-http-request,
  which requires a 3 arg serialize fn."
  (fn [_ shape _data] (:type shape)))

(defmethod serialize :default
  [_ shape data]
  (shape/json-serialize shape data))

(defmethod serialize "structure"
  [_ shape data]
  (some->> (util/with-defaults shape data)
           not-empty
           (shape/json-serialize shape)))

(defmethod serialize "timestamp"
  [_ shape data]
  (shape/format-date shape data))

(defmethod aws.protocols/build-http-request "rest-json"
  [service op-map]
  (rest/build-http-request service op-map serialize))

(defmethod aws.protocols/parse-http-response "rest-json"
  [service op-map http-response]
  (rest/parse-http-response service
                            op-map
                            http-response
                            shape/json-parse
                            aws.protocols/json-parse-error))
