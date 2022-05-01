;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns ^:skip-wiki com.grzm.awyeah.protocols.rest-json
  "Impl, don't call directly."
  (:require
   [clojure.string :as str]
   [com.grzm.awyeah.client :as client]
   [com.grzm.awyeah.json :as json]
   [com.grzm.awyeah.protocols.common :as common]
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

(defmethod client/build-http-request "rest-json"
  [service op-map]
  (rest/build-http-request service op-map serialize))

(defmulti parser (fn [http-response] (get-in http-response [:headers "content-type"])))

(defmethod parser :default [_] shape/json-parse)

(defmethod parser "application/hal+json" [_]
  (fn [shape body-str]
    (when-not (str/blank? body-str)
      (let [data (json/read-str body-str)]
        (->> (into (dissoc data :_embedded :_links)
                   (some->> (get data :_embedded)
                            (reduce-kv (fn [m k v]
                                         (assoc m
                                                k
                                                (if (sequential? v)
                                                  (mapv #(dissoc % :_links) v)
                                                  (dissoc v :_links))))
                                       {})))
             ;; TODO (dchelimsky 2019-01-09) using json-parse* to
             ;; avoid str -> data -> str -> data, but maybe that fn
             ;; needs a diff name?
             (shape/json-parse* shape))))))

(defmethod client/parse-http-response "rest-json"
  [service op-map http-response]
  (rest/parse-http-response service
                            op-map
                            http-response
                            (parser http-response)
                            common/json-parse-error))
