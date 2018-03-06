(ns nashorn.client.app
  (:require
   [nashorn.client.editor :refer [Editor]]
   [nashorn.client.import-pane :refer [ImportPane]]
   [nashorn.client.properties :refer [NewProp EditProp Properties]]
   [nashorn.client.scripts :refer [Scripts]]
   [nashorn.client.sidebar :refer [SideBar]]
   [nashorn.client.ui :refer [do-send! send! PureMixin WorkArea]]
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

(def ^:private editing?
  #{:view/new-script :view/edit-script
    :view/props-edit :view/props-new})

(defcs RootUI < PureMixin (rum/local false :this/importing?)
  [ls state ch]
  [:section.App {:onDragOver #(reset! (:this/importing? ls) true)}
   (SideBar state (editing? (:view state)) ch)
   (UIFrame state ch)
   (when @(:this/importing? ls)
     (ImportPane #(reset! (:this/importing? ls) false) ch))])

(defn render!
  [state ch]
  (rum/mount (RootUI state ch) (.getElementById js/document "app")))
