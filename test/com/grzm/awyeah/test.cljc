(ns com.grzm.awyeah.test
  (:require
   [clojure.pprint :as pprint]
   [clojure.test :as test]))

#?(:bb (taoensso.timbre/set-level! :info))

(def test-namespaces
  '[com.grzm.awyeah.client.api-test
    com.grzm.awyeah.client.api.localstack-test
    com.grzm.awyeah.client-test])

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
