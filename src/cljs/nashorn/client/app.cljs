(ns nashorn.client.app
  (:require
   [nashorn.client.editor :refer [Editor]]
   [nashorn.client.import-pane :refer [ImportPane]]
   [nashorn.client.properties :refer [NewProp EditProp Properties]]
   [nashorn.client.scripts :refer [Scripts]]
   [nashorn.client.sidebar :refer [SideBar]]
   [nashorn.client.ui :refer [do-send! send! Button Container IncludeIf PureMixin]]
   [rum.core :as rum :refer [defc defcs]]))

(defn- find-focus
  [{focus :script/focus, list :script/list}]
  (if (nil? focus)
    nil
    (first (filter #(= (:id %) focus) list))))

(defn- find-prop
  [{props :props/list, key :props/focus}]
  (if (nil? key)
    nil
    (reduce #(if (= (:key %2) key) (reduced %2) nil) nil props)))

(defc UIFrame < PureMixin
  [state ch]
  (let [script     (find-focus state)
        run-result (:script/test-result state)]
    (case (:view state)
      :view/new-script  (Editor {} run-result ch)
      :view/edit-script (Editor script run-result ch)
      :view/props-home  (Properties (:props/list state) (:props/focus state) ch)
      :view/props-edit  (EditProp (find-prop state) ch)
      :view/props-new   (NewProp {:key "" :value ""} ch)
      (Scripts (:script/list state) script run-result
               (:script/logs state) ch))))

(defc ErrorPane
  [error ch]
  [:div.ErrorPane
   [:div.Header
    [:div.Title "Server error"]
    [:div.Commands
     (Button {:type :close :onClick (send! ch :error/dismiss)})]]
   [:div.Body
    [:p "code:" (:code error)]
    [:p "reason:" (:reason error)]]])

(def ^:private editing?
  #{:view/new-script :view/edit-script
    :view/props-edit :view/props-new})

(defcs RootUI < PureMixin (rum/local false :this/importing?)
  [ls state ch]
  (let [error?     (not (nil? (:server/error state)))
        importing? @(:this/importing? ls)]
    (Container "App" {:onDragOver #(reset! (:this/importing? ls) true)}
      (IncludeIf
        :always    (SideBar state (editing? (:view state)) ch)
        :always    (UIFrame state ch)
        error?     (ErrorPane (:server/error state) ch)
        importing? (ImportPane #(reset! (:this/importing? ls) false) ch)))))

(defn render!
  [state ch]
  (rum/mount (RootUI state ch) (.getElementById js/document "app")))
