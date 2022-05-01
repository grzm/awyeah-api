(ns com.grzm.awyeah.http.awyeah
  (:require
   [com.grzm.awyeah.http :as aws]
   [com.grzm.awyeah.http-client :as impl]))

(set! *warn-on-reflection* true)

(defn create
  []
  (let [c (impl/create nil)]
    (reify aws/HttpClient
      (-submit [_ request channel]
        (impl/submit c request channel))
      (-stop [_]
        (impl/stop c)))))
