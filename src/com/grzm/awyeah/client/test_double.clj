;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns com.grzm.awyeah.client.test-double
  "Provides a test implementation of the aws client, which can be passed
  to the functions in the com.grzm.awyeah.client.api ns."
  (:require
   [clojure.core.async :as a]
   [com.grzm.awyeah.client.protocol :as client.protocol]
   [com.grzm.awyeah.client.validation :as validation]
   [com.grzm.awyeah.service :as service]))

(set! *warn-on-reflection* true)

(defn ^:private no-handler-provided-anomaly [op]
  {:cognitect.anomalies/category :cognitect.anomalies/incorrect
   :cognitect.anomalies/message  "No handler or response provided for op"
   :op op})

(defrecord Client [info handlers]
  client.protocol/Client
  (-get-info [_] info)

  (-invoke [this {:keys [op request] :as op-map}]
    (let [spec (validation/request-spec (:service info) op)]
      (cond
        (not (get-in info [:service :operations op]))
        (validation/unsupported-op-anomaly (:service info) op)

        (and (validation/validate-requests? this)
             spec
             (not (validation/valid? spec request)))
        (validation/invalid-request-anomaly spec request)

        (not (get handlers op))
        (no-handler-provided-anomaly op)

        :else
        (let [handler (get handlers op)]
          (handler op-map)))))

  (-invoke-async [this {:keys [ch] :as op-map}]
    (let [response-chan (or ch (a/promise-chan))]
      (a/go
        (let [resp (client.protocol/-invoke this op-map)]
          (a/>! response-chan resp)))
      response-chan))

  (-stop [_aws-client]))

;; ->Client is intended for internal use
(alter-meta! #'->Client assoc :skip-wiki true)

(defn client
  "Given a map with :api and :ops, returns a test client that you can
  pass to `com.grzm.awyeah.client.api/invoke` and
  `com.grzm.awyeah.client.api/stop` in implementation code.

  :ops should be a map of operation to one of
  - handler function of op-map that returns a response map
  - literal response map

  Notes:
  - you must declare every op that will be invoked during a test
  - every op must be supported
    - See (keys (com.grzm.awyeah.client.api/ops test-client))
  - will validate request payloads passed to `invoke` by default
    - you can disable request validation with (com.grzm.awyeah.client.api/validate-requests client false)
  - will not validate response payloads"
  [{:keys [api ops]}]
  (let [service (service/service-description (name api))]
    (-> (->Client {:service service
                   :validate-requests? (atom true)}
                  (reduce-kv
                    (fn [m op response]
                      (when-not (some-> service :operations op)
                        (throw (ex-info "Operation not supported"
                                        (validation/unsupported-op-anomaly service op))))
                      (assoc m op (if (fn? response) response (constantly response))))
                    {}
                    ops))
        (assoc :api (-> service :metadata :cognitect.aws/service-name)
               :service service))))
