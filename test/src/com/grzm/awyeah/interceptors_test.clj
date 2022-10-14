;; Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns com.grzm.awyeah.interceptors-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.grzm.awyeah.interceptors :as interceptors]))

(deftest test-apigatewaymanagementapi
  (is (= "prefixsuffix"
         (:uri
          (interceptors/modify-http-request {:metadata {:uid "apigatewaymanagementapi"}}
                                            {:op :PostToConnection
                                             :request {:ConnectionId "suffix"}}
                                            {:uri "prefix"})))))
