(ns com.grzm.awyeah.test
  (:require
   [clojure.pprint :as pprint]
   [clojure.test :as test]))

#?(:bb (taoensso.timbre/set-level! :info))

(def test-namespaces
  '[com.grzm.awyeah.client.api.localstack-test
    com.grzm.awyeah.client.api-test
    com.grzm.awyeah.client.impl-test
    com.grzm.awyeah.client.test-double-test
    com.grzm.awyeah.config-test
    com.grzm.awyeah.credentials-test
    com.grzm.awyeah.ec2-metadata-utils-test
    com.grzm.awyeah.endpoint-test
    com.grzm.awyeah.interceptors-test
    com.grzm.awyeah.protocols-test
    com.grzm.awyeah.protocols.rest-test
    com.grzm.awyeah.region-test
    com.grzm.awyeah.retry-test
    com.grzm.awyeah.shape-test
    ;; omitting com.grzm.awyeah.signers-test
    ;; Requires org.apache.commons.io.input.BOMInputStream which I haven't figured out
    ;; how to port to something compatible with Babashka
    com.grzm.awyeah.util-test])

(defn run-tests
  ([]
   (run-tests {:test-namespaces test-namespaces}))
  ([{nses :test-namespaces}]
   (dorun (map require nses))
   (let [res (apply test/run-tests nses)]
     (pprint/pprint res)
     (when (->> ((juxt :fail :error) res)
                (some #(pos? %)))
       (System/exit 1)))))
