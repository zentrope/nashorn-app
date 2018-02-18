(ns nashorn.client.app
  (:require
   [cljs.core.async :refer [put!]]
   [rum.core :as rum :refer [defc]]))

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

(defn render!
  [state ch]
  (rum/mount (RootUI state ch) (.getElementById js/document "app")))
