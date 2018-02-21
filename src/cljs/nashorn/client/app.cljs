(ns nashorn.client.app
  (:require
   [nashorn.client.editor :refer [Editor]]
   [nashorn.client.sidebar :refer [SideBar]]
   [nashorn.client.ui :refer [send! PureMixin WorkArea]]
   [rum.core :as rum :refer [defc]]))

(defc AllScripts < PureMixin
  [ch]
  (WorkArea
   [:section
    [:h1 "Scripts"]
    [:p "Not implemented."]]))

(defc UIFrame < PureMixin
  [state ch]
  (case (:view state)
    :view/new-script (Editor state ch)
    (AllScripts ch)))

(defc RootUI < PureMixin
  [state ch]
  [:section.App
   (SideBar state (= (:view state) :view/new-script) ch)
   (UIFrame state ch)])

(defn render!
  [state ch]
  (rum/mount (RootUI state ch) (.getElementById js/document "app")))
