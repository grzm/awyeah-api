;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns com.grzm.awyeah.client.shared
  (:require
   [com.grzm.awyeah.credentials :as credentials]
   [com.grzm.awyeah.http :as http]
   [com.grzm.awyeah.region :as region]))

(set! *warn-on-reflection* true)

(declare http-client)

(def ^:private shared-http-client
  (delay (http/resolve-http-client nil)))

(def ^:private shared-credentials-provider
  (delay (credentials/default-credentials-provider (http-client))))

(def ^:private shared-region-provider
  (delay (region/default-region-provider (http-client))))

(defn http-client
  "Returns the globally shared instance of http-client (created on the
  first call).

  Alpha. Subject to change."
  []
  @shared-http-client)

(defn credentials-provider
  "Returns the globally shared instance of credentials-provider, which
  uses the globally shared instance of http-client.

  Alpha. Subject to change."
  []
  @shared-credentials-provider)

(defn region-provider
  "Returns the globally shared instance of region-provider, which
  uses the globally shared instance of http-client.

  Alpha. Subject to change."
  []
  @shared-region-provider)

(defn ^:private shared-http-client?
  "For internal use.

  Alpha. Subject to change."
  [candidate-http-client]
  (identical? candidate-http-client
              (and (realized? shared-http-client) @shared-http-client)))
