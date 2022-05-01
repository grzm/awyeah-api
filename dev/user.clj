(ns user
  (:require
   [com.grzm.awyeah.client.api :as aws]
   [com.grzm.awyeah.client :as client]
   [com.grzm.awyeah.credentials :as credentials]
   [com.grzm.awyeah.endpoint :as endpoint]))

;; localstack
(def client-default-opts
  {:credentials-provider (credentials/basic-credentials-provider
                           {:access-key-id "ABC"
                            :secret-access-key "XYZ"})
   :endpoint-override {:protocol :http
                       :hostname "localhost"
                       :port 4566}})

(defn client [opts]
  (aws/client (merge client-default-opts opts)))

(comment
  
  (def sts (aws/client {:api :sts}))
  (def sts (client {:api :sts}))

  (aws/invoke sts {:op :GetCallerIdentity})
  
  (keys (client/-get-info sts))
  (def client-info (client/-get-info sts))
  (:endpoint-provider client-info)
  (def service (:service (client/-get-info sts)))
  (def req-map {:op :GetCallerIdentity})
  (client/build-http-request service req-map)
  (endpoint/default-endpoint-provider (get-in service [:metadata :endpointPrefix]) {})

  
  (def s3 (aws/client {:api :s3}))
  (aws/invoke s3 {:op :ListBuckets})
  (def ec2 (aws/client {:api :ec2, :region "eu-west-1"}))
  (aws/invoke ec2 {:op :DescribeInstances})

  (def lambda (aws/client {:api :lambda}))
  (aws/invoke lambda {:op :ListFunctions})
  (def ssm (aws/client {:api :ssm}))

  (def s3 (client {:api :s3}))

  (aws/invoke sts {:op :GetCallerIdentity})

  (def s3 (aws/client {:api :s3}))

  (def req-map {:op :ListBuckets})
  (:service (client/-get-info s3))
  (do (println "----")
      (client/build-http-request (:service (client/-get-info s3)) req-map))
  (aws/invoke s3 req-map)
  
  :end)

