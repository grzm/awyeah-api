;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns ^:skip-wiki com.grzm.awyeah.client.api.async
  "DEPRECATED"
  (:require
   [com.grzm.awyeah.client.protocol :as client.protocol]))

(defn ^:deprecated invoke
  "DEPRECATED: use com.grzm.awyeah.client.api/invoke-async instead"
  [client op-map]
  (client.protocol/-invoke-async client op-map))
