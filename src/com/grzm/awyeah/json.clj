(ns com.grzm.awyeah.json
  (:require [cheshire.core :as json]))

(defn write-str [x]
  (json/generate-string x))

(defn read-str [s]
  (json/parse-string s keyword))
