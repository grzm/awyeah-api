(ns com.grzm.awyeah.integration.error-codes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.grzm.awyeah.client.api :as aws]
   [com.grzm.awyeah.client.api.localstack-elf :as localstack-elf]))

(defn client [opts]
  (aws/client (merge localstack-elf/client-default-opts opts)))

(deftest ^:integration error-codes-for-protocols
  (testing "rest-xml"
    (is (= "NoSuchBucket"
           (:cognitect.aws.error/code
            (aws/invoke (client {:api :s3})
                        {:op :GetObject
                         :request {:Bucket "i-do-not-exist"
                                   :Key "neither-do-i.txt"}})))))

  (testing "rest-json"
    (is (= "ValidationException"
           (:cognitect.aws.error/code
            (aws/invoke (client {:api :lambda})
                        {:op :CreateFunction
                         :request {:FunctionName "some-function-name"
                                   :Role "not-a-role-arn"
                                   :Code {}}})))))

  (testing "ec2"
    (is (= "MissingParameter"
           (:cognitect.aws.error/code
            (aws/invoke (client {:api :ec2})
                        {:op :CreateLaunchTemplate})))))

  ;; autoscaling isn't supported in community edition of Localstack
  #_(testing "query"
      (is (= "ValidationError"
             (:cognitect.aws.error/code
              (aws/invoke (client {:api :autoscaling})
                          {:op :PutScalingPolicy})))))

  (testing "json"
    (is (= "ValidationException"
           (:cognitect.aws.error/code
            (aws/invoke (client {:api :ssm})
                        {:op :PutParameter
                         :request {:Name "ssm"}}))))))
