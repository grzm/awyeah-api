(ns com.grzm.awyeah.client.api-test
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.test :as test :refer [deftest is testing]]
   [cognitect.anomalies :as-alias anom]
   [com.grzm.awyeah.client.api :as aws]
   [com.grzm.awyeah.credentials :as credentials]
   [com.grzm.awyeah.http.awyeah :as http.awyeah]))

(defn anomaly? [x]
  (when (and (map? x) (:cognitect.anomalies/category x))
    x))

(def client-default-opts
  {:credentials-provider (credentials/basic-credentials-provider
                           {:access-key-id "ABC"
                            :secret-access-key "XYZ"})
   :endpoint-override {:protocol :http
                       :hostname "localhost"
                       :port 4566}})

(defn client [opts]
  (aws/client (merge client-default-opts opts)))

(defn test-with-http-client
  [http-client label]

  (testing "ec2 (ec2 protocol)"
    (let [ec2 (client (cond-> {:api :ec2}
                        http-client (assoc :http-client http-client)))
          keypair-name label]
      (let [res (aws/invoke ec2 {:op :DescribeKeyPairs})]
        (is (nil? (anomaly? res)))
        (is (nil? (->> (:KeyPairs res)
                       (some #(= (:KeyName %) keypair-name))))))
      (let [res (aws/invoke ec2 {:op :CreateKeyPair, :request {:KeyName keypair-name}})]
        (is (= keypair-name (:KeyName res))))
      (let [res (aws/invoke ec2 {:op :DescribeKeyPairs})]
        (is (nil? (anomaly? res)))
        (is (true? (->> (:KeyPairs res)
                        (some #(= (:KeyName %) keypair-name))))))))

  (testing "lambda (rest-json protocol)"
    (let [lambda (client (cond-> {:api :lambda}
                           http-client (assoc :http-client http-client)))
          res (aws/invoke lambda {:op :ListFunctions})]
      (is (nil? (anomaly? res)))))

  (testing "sts (query protocol)"
    (let [sts (client (cond-> {:api :sts}
                        http-client (assoc :http-client http-client)))]
      (is (= #{:UserId :Account :Arn} (set (keys (aws/invoke sts {:op :GetCallerIdentity})))))))

  (testing "ssm (json protocol"
    (let [ssm (client (cond-> {:api :ssm}
                        http-client (assoc :http-client http-client)))
          root (str "/" label)
          foo-param-name (str root "/" "foo")
          foo-param-value "yada-foo"]
      (let [{:keys [Parameters]} (aws/invoke ssm {:op :GetParametersByPath
                                                  :request {:Path root
                                                            :Recursive true}})]
        (is (= [] Parameters)))
      (is (= 1 (:Version (aws/invoke ssm {:op :PutParameter
                                          :request {:Name foo-param-name
                                                    :Type "String"
                                                    :Value foo-param-value}}))))
      (is (= [{:Type "String"
               :Name foo-param-name
               :Value foo-param-value}]
             (->> (aws/invoke ssm {:op :GetParametersByPath
                                   :request {:Path root
                                             :Recursive true}})
                  :Parameters
                  (mapv #(select-keys % [:Type :Name :Value])))))))

  (testing "s3 (rest-xml protocol)"
    (let [s3 (client (cond-> {:api :s3}
                       http-client (assoc :http-client http-client)))
          bucket-name (str "some-bucket-" label)
          greeting-key "greeting"
          greeting-body "hai"
          farewell-key "farewell"
          farewell-body "kthxbye"]
      (let [res (aws/invoke s3 {:op :CreateBucket,
                                :request {:Bucket bucket-name}})]
        (is (= (str "/" bucket-name) (:Location res))))
      (testing "bytes body"
        (is (= :ETag (->> (aws/invoke s3 {:op :PutObject,
                                          :request {:Bucket bucket-name,
                                                    :Key greeting-key
                                                    :Body (.getBytes greeting-body)}})
                          keys
                          (some #{:ETag}))))
        (let [{:keys [Body]} (aws/invoke s3 {:op :GetObject
                                             :request {:Bucket bucket-name
                                                       :Key greeting-key}})]
          (is (= greeting-body (slurp Body)))))
      (testing "input-stream body"
        (is (= :ETag (->> (aws/invoke s3 {:op :PutObject,
                                          :request {:Bucket bucket-name
                                                    :Key farewell-key
                                                    :Body (io/input-stream (.getBytes farewell-body))}})
                          keys
                          (some #{:ETag}))))
        (let [{:keys [Body]} (aws/invoke s3 {:op :GetObject
                                             :request {:Bucket bucket-name
                                                       :Key farewell-key}})]
          (is (= farewell-body (slurp Body)))))
      (testing "HeadObject on non-existant object"
        (is (= ::anom/not-found (::anom/category
                                 (aws/invoke s3 {:op :HeadObject,
                                                 :request {:Bucket bucket-name,
                                                           :Key "no-such-object"}}))))))))

(defn gen-label
  ([]
   (gen-label (java.time.Instant/now)))
  ([as-of]
   (java.lang.Long/toString (.getEpochSecond as-of) 26)))

(deftest awyeah-http-client-test
  (let [http-client (http.awyeah/create)
        label (str "java.net.http-" (gen-label))]
    (testing "java.net.http"
      (test-with-http-client http-client label))))

(deftest default-client-test
  (let [label (str "default-http-client-" (gen-label))]
    (testing "default-http-client"
      (test-with-http-client nil label))))

(comment
  (test/run-tests)
  :end)

(def this-ns *ns*)

(defn run-tests
  ([]
   (run-tests {:test-ns this-ns}))
  ([{:keys [test-ns]}]
   (let [res (test/run-tests test-ns)]
     (pprint/pprint
       res)
     (when (->> ((juxt :fail :error) res)
                (some #(< 0 %)))
       (System/exit 1)))))
