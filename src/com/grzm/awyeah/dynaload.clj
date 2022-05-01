(ns ^:skip-wiki com.grzm.awyeah.dynaload)

(set! *warn-on-reflection* true)

(defonce ^:private dynalock (Object.))

(defn load-ns [ns]
  (locking dynalock
    (require (symbol ns))))
