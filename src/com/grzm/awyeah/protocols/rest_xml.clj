;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns ^:skip-wiki com.grzm.awyeah.protocols.rest-xml
  "Impl, don't call directly."
  (:require
   [com.grzm.awyeah.client :as client]
   [com.grzm.awyeah.protocols.common :as common]
   [com.grzm.awyeah.protocols.rest :as rest]
   [com.grzm.awyeah.shape :as shape]))

(set! *warn-on-reflection* true)

(defn serialize
  "xml body args serializer passed to rest/build-http-request"
  [shape-name shape data]
  (when data
    (shape/xml-serialize shape
                         data
                         (or (:locationName shape) shape-name))))

(comment
  (defn spy-> [x label]
    (tap> {label x})
    x)

  (add-tap prn)

  :end)

(defmethod client/build-http-request "rest-xml"
  [service op-map]
  (tap> {:rest-xml.build-http-request/op-map op-map})
  (rest/build-http-request service op-map serialize))

(defmethod client/parse-http-response "rest-xml"
  [service op-map http-response]
  (rest/parse-http-response service
                            op-map
                            http-response
                            shape/xml-parse
                            common/xml-parse-error))
