(ns nashorn.client.sidebar
  (:require
   [nashorn.client.icon :as icon]
   [nashorn.client.ui :refer [send! Button PureMixin]]
   [rum.core :refer [defc]]))

(defn- script-item
  [script selected? ch]
  [:div {:key (:id script)
         :class ["Item" (if selected? "Selected")]
         :onClick (send! ch :script/focus {:id (:id script)})}
   [:div.Icon.Fn (icon/Script)]
   [:div.Label (:name script)]])

(defn- fn-item
  [{:keys [name] :as function-doc} ch]
  [:div.Item {:key name
              :onClick (send! ch :function/show {:doc function-doc})}
   [:div.Icon.Fn "\u03bb"]
   [:div.Label name]])

(defc SideBarScriptsPanel < PureMixin
  [scripts focus ch]
  [:section.Panel
   [:div.Title.Active {:onClick (send! ch :script/home)}
    "Extensions"]
   [:div.Body
    (for [script (sort-by :name scripts)]
      (script-item script (= focus (:id script)) ch))]])

(defc DocumentationPanel < PureMixin
  [documentation ch]
  [:section.Panel
   [:div.Title "Documentation"]
   [:div.Body
    (for [f (sort-by :name (vals documentation))]
      (fn-item f ch))]])

(defc PropertyPanel < PureMixin
  [properties focus editing? ch]
  [:section.Panel
   (if editing?
     [:div.Title "Properties"]
     [:div.Title.Active {:onClick (send! ch :props/home)} "Properties"])
   [:div.Body
    (for [property (sort-by :key properties)]
      [:div (cond-> {:key (:key property)
                     :class ["Item" (when (= (:key property) focus) "Selected")]}
              (not editing?) (assoc :onClick (send! ch :props/focus {:key (:key property)})))
       [:div.Icon.Env (icon/Env)]
       [:div.Label (:key property)]]
      )]])

(defc SideBar < PureMixin
  [state editing? ch]
  [:section.SideBar
   [:div.Header
    [:div.Title "Script Console"]]
   [:div.Panels
    (if editing?
      (DocumentationPanel (:script/docs state) ch)
      (SideBarScriptsPanel (sort-by :name (:script/list state)) (:script/focus state) ch))
    (PropertyPanel (:props/list state) (:props/focus state) editing? ch)]
   [:div.Buttons
    (Button {:type :new
             :label "Script"
             :disabled? editing?
             :onClick (send! ch :script/new)})
    (Button {:type :new
             :label "Property"
             :disabled? editing?
             :onClick (send! ch :props/new)})]])
