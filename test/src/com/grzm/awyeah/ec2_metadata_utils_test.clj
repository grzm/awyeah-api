;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns com.grzm.awyeah.ec2-metadata-utils-test
  (:require
   [clojure.core.async :as a]
   [clojure.test :refer [deftest is use-fixtures]]
   [com.grzm.awyeah.client.shared :as shared]
   [com.grzm.awyeah.ec2-metadata-utils :as ec2-metadata-utils]
   [com.grzm.awyeah.http :as http]
   [com.grzm.awyeah.test.ec2-metadata-utils-server :as ec2-metadata-utils-server]))

(def ^:dynamic *test-server-port*)
(def ^:dynamic *http-client*)

(defn test-server
  [f]
  ;; NOTE: starting w/ 0 generates a random port
  (let [server-stop-fn   (ec2-metadata-utils-server/start 0)
        test-server-port (-> server-stop-fn meta :local-port)]
    (try
      (System/setProperty ec2-metadata-utils/ec2-metadata-service-override-system-property
                          (str "http://localhost:" test-server-port))
      (binding [*test-server-port* test-server-port
                *http-client*      (shared/http-client)]
        (f))
      (finally
        (server-stop-fn)
        (System/clearProperty ec2-metadata-utils/ec2-metadata-service-override-system-property)))))

(use-fixtures :once test-server)

(deftest returns-nil-after-retries
  (with-redefs [http/submit (constantly
                              (doto (a/promise-chan)
                                (a/>!! {:cognitect.anomalies/category :cognitect.anomalies/busy})))]
    (is (nil? (ec2-metadata-utils/get-ec2-instance-region *http-client*)))))
