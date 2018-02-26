(ns nashorn.client.sidebar
  (:require
   [nashorn.client.ui :refer [send! Button PureMixin]]
   [rum.core :refer [defc]]))

(defn- script-item
  [script selected? ch]
  [:div {:key (:id script)
         :class ["Item" (if selected? "Selected")]
         :onClick (send! ch :script/focus {:id (:id script)})}
   [:span.Script "= "] (:name script)])

(defn- fn-item
  [{:keys [name] :as function-doc} ch]
  [:div.Item {:key name
              :onClick (send! ch :function/show {:doc function-doc})}
   [:span.Fn "\u03bb "] name])

(def ^:private sb-header
  [:div.Header [:div.Title "Script Console"]])

(defc SideBarScriptsPanel < PureMixin
  [scripts focus ch]
  [:section.Panel
   [:div.Title "Extensions"]
   [:div.Body
    (for [script (sort-by :name scripts)]
      (script-item script (= focus (:id script)) ch))]])

(defc SideBarFunctionsPanel < PureMixin
  [functions ch]
  [:section.Panel
     [:div.Title "Documentation"]
   [:div.Body
    (for [f (sort-by :name (vals functions))]
      (fn-item f ch))]])

(defc SideBar < PureMixin
  [state editing? ch]
  [:section.SideBar sb-header
   [:div.Panels
    (if editing?
      (SideBarFunctionsPanel (:script/docs state) ch)
      (SideBarScriptsPanel (sort-by :name (:script/list state))
                           (:script/focus state) ch))]
   [:div.Buttons
    (Button {:type :new
             :onClick (send! ch :script/new)
             :disabled? editing?
             :label "New Script"})]])
