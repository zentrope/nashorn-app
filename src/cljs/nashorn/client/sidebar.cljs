(ns nashorn.client.sidebar
  (:require
   [nashorn.client.ui :refer [send! Button PureMixin]]
   [rum.core :refer [defc]]))

(defn- script-item
  [name ch]
  [:div.Item {:key name} [:span.Script "= "] name])

(defn- fn-item
  [{:keys [name] :as function-doc} ch]
  [:div.Item {:key name
              :onClick (send! ch :function/show {:function function-doc})}
   [:span.Fn "\u03bb "] name])

(def ^:private sb-footer
  [:div.Footer [:div.Copyright "\u00a9 2018 Tripwire Inc"]])

(def ^:private sb-header
  [:div.Header [:div.Title "Extension Editor"]])

(defc SideBarScriptsPanel < PureMixin
  [scripts ch]
  [:section.Panel
   [:div.Title "Scripts"]
   [:div.Body
    (for [script (sort scripts)]
      (script-item script ch))]])

(defc SideBarFunctionsPanel < PureMixin
  [functions ch]
  [:section.Panel
     [:div.Title "Documentation"]
   [:div.Body
    (for [f (sort-by :name (vals functions))]
      (fn-item f ch))]])

(defc SideBar < PureMixin
  [state ch]
  [:section.SideBar
   sb-header
   [:div.Panels
    (SideBarScriptsPanel ["github.js"] ch)
    (SideBarFunctionsPanel (:functions state) ch)]
   [:div.Buttons
    (when-not (= (:view state) :view/new-script)
      (Button {:onClick (send! ch :script/new) :label "New Script"}))]
   sb-footer])
