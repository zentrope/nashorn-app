(ns nashorn.client.main
  (:require
   [clojure.string :as string]
   [cljs.core.async :refer [chan go-loop put!]]
   [nashorn.client.app :as app]
   [nashorn.client.event :as event]
   [nashorn.client.http :as http]
   [rum.core :as rum :refer [defc]]))

(enable-console-print!)

;; app state

(defonce app-state
  (atom {:script/docs        {}  ; help for script extension functions
         :script/list        []  ; list of saved scripts
         :script/test-result nil ; result form testing a script
         :script/focus       nil ; currently focussed script
         :script/logs        []  ; run logs for a given script
         :props/list         []  ; environment vars
         :props/focus        nil ; currently focussed property
         :view               :view/home}))

(defn reload
  []
  (println "Reload called."))

(defn main
  []
  (println "Welcome to the Extension Manager")
  (let [ch (chan)]
    (add-watch app-state :state (fn [k r o n] (app/render! n ch)))
    (http/send! ch {:event :script/list})
    (http/send! ch {:event :props/list})
    (app/render! @app-state ch)
    (event/loop! ch #(reset! app-state (event/mutate! @app-state ch %)))))

(main)
