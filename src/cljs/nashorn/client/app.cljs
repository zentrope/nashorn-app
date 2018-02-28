(ns nashorn.client.app
  (:require
   [nashorn.client.editor :refer [Editor]]
   [nashorn.client.scripts :refer [Scripts]]
   [nashorn.client.sidebar :refer [SideBar]]
   [nashorn.client.ui :refer [send! PureMixin WorkArea]]
   [rum.core :as rum :refer [defc]]))


(defn- find-focus
  [{:keys [script/focus :script/list]}]
  (if (nil? focus)
    nil
    (first (filter #(= (:id %) focus) list))))

(defc UIFrame < PureMixin
  [state ch]
  (let [script (find-focus state)
        run-result (:script/test-result state)]
    (case (:view state)
      :view/new-script  (Editor {} run-result ch)
      :view/edit-script (Editor script run-result ch)
      ;; :view/home
      (Scripts (:script/list state)
               script
               run-result
               ch))))

(def ^:private editing?
  #{:view/new-script :view/edit-script})

(defc RootUI < PureMixin
  [state ch]
  [:section.App
   (SideBar state (editing? (:view state)) ch)
   (UIFrame state ch)])

(defn render!
  [state ch]
  (rum/mount (RootUI state ch) (.getElementById js/document "app")))
