(ns nashorn.app.main
  (:require
   [clojure.string :as string]
   [cljs.core.async :refer [chan go-loop put!]]
   [rum.core :as rum :refer [defc]]))

(enable-console-print!)

;; component helpers

(defn send!
  [ch event & [msg]]
  (fn [e]
    (let [msg (if (nil? msg) {} msg)]
      (put! ch (assoc msg :event event)))))

;; state rendering

(defc RootUI < rum/static
  [state ch]
  [:section
   [:h1 (:title state)]
   [:p "This is all there is at the moment."]
   [:button {:onClick (send! ch :whimsy/do {:x 1})} "Test event system"]
   [:button {:onClick (send! ch :whimsy/undo)} "Reset"]])

;; state mutation (event) handlers

(defmulti mutate!
  (fn [state msg]
    (:event msg)))

(defmethod mutate! :default
  [state msg]
  (println "Unable to process message:" (pr-str msg))
  state)

(defmethod mutate! :whimsy/do
  [state msg]
  (assoc state :title (str (:title state) "!")))

(defmethod mutate! :whimsy/undo
  [state msg]
  (assoc state :title (string/replace (:title state) "!" "")))

;; async event processing loop

(defn event-loop!
  [ch f]
  (go-loop []
    (when-let [msg (<! ch)]
      (try
        (f msg)
        (catch js/Error e
          (println "ERROR:" (pr-str {:error e :msg msg}))))
      (recur))))

;; app state

(defonce app-state
  (atom {:title "Extension Manager"}))

(defn render!
  [state ch]
  (rum/mount (RootUI state ch) (.getElementById js/document "app")))

(defn reload
  []
  (println "Reload called."))

(defn main
  []
  (println "Welcome to the Extension Manager")
  (let [ch (chan)]
    (add-watch app-state :state (fn [k r o n] (render! n ch)))
    (render! @app-state ch)
    (event-loop! ch #(reset! app-state (mutate! @app-state %)))))

(main)
