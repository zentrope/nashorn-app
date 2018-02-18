(ns nashorn.client.main
  (:require
   [clojure.string :as string]
   [cljs.core.async :refer [chan go-loop put!]]
   [nashorn.client.app :as app]
   [nashorn.client.event :as event]
   [rum.core :as rum :refer [defc]]))

(enable-console-print!)

;; app state


(def ^:private function-docs
  {"format"   {:name "format"
               :desc "Format string templates in an sprintf way."}
   "httpGet"  {:name "httpGet"}
   "httpPost" {:name "httpPost"}
   "lookup"   {:name "lookup"}})

(defonce app-state
  (atom {:title     "Extension Editor"
         :functions function-docs}))

(defn reload
  []
  (println "Reload called."))

(defn main
  []
  (println "Welcome to the Extension Manager")
  (let [ch (chan)]
    (add-watch app-state :state (fn [k r o n] (app/render! n ch)))
    (app/render! @app-state ch)
    (event/loop! ch #(reset! app-state (event/mutate! @app-state %)))))

(main)
