(ns com.grzm.awyeah.client.api.localstack-elf
  (:require
   [com.grzm.awyeah.credentials :as credentials]))

(def client-default-opts
  {:credentials-provider (credentials/basic-credentials-provider
                           {:access-key-id "ABC"
                            :secret-access-key "XYZ"})
   :endpoint-override {:protocol :http
                       :hostname "localhost"
                       :port 4566}})
