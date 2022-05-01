;; Copyright (c) Michael Glaesemann
;; Heavily inspired by congitect.http-client, Copyright (c) Cognitect, Inc.
;; All rights reserved.

(ns com.grzm.awyeah.http-client.client)

(defprotocol Client
  (-submit [_ request ch]))

(defn submit [client request ch]
  (-submit client request ch))
