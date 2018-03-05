(ns user
  (:require
   [integrant.repl :refer [clear go halt prep init reset reset-all]]
   [nashorn.server.main :as main]))

(integrant.repl/set-prep! (constantly main/config))

(defn restart []
  (halt)
  (go))

;; To start:
;;  (go)
;;
;; To stop:
;;  (halt)
;;
;; To reset (reload source files):
;;  (reset)
;;
