(ns nashorn.client.app
  (:require
   [cljs.core.async :refer [put!]]
   [rum.core :as rum :refer [defc]]))

;; component helpers

(defn send!
  [ch event & [msg]]
  (fn [e]
    (let [msg (if (nil? msg) {} msg)]
      (put! ch (merge {:event event} msg)))))

(def PureMixin rum/static)

(defn- script-item
  [name]
  [:div.Item [:span.Script "= "] name])

(defn- fn-item
  [{:keys [name] :as function-doc} ch]
  [:div.Item {:key name
              :onClick (send! ch :function/show function-doc)}
   [:span.Fn "\u03bb "] name])

(def ^:private sb-footer
  [:div.Footer [:div.Copyright "\u00a9 2018 Tripwire Inc"]])

(def ^:private sb-header
  [:div.Header [:div.Title "Extension Editor"]])

(defc SideBarFunctionsPanel < PureMixin
  [functions ch]
  [:section.Panel
     [:div.Title "Functions"]
   [:div.Body
    (for [f (sort-by :name (vals functions))]
      (fn-item f ch))]])

(defc SideBar < PureMixin
  [state ch]
  [:section.SideBar
   sb-header
   [:div.Panels
    [:section.Panel
     [:div.Title "Scripts"]
     [:div.Body
      (script-item "github.js")]]
    (SideBarFunctionsPanel (:functions state) ch)]
   sb-footer
   ])

(defc WorkArea < PureMixin
  [state ch]
  [:section.WorkArea
   [:h1 (:title state)]
   [:p "This is all there is at the moment."]
   [:button {:onClick (send! ch :whimsy/do {:x 1})} "Test event system"]
   [:button {:onClick (send! ch :whimsy/undo)} "Reset"]
   ])

(defc RootUI < PureMixin
  [state ch]
  [:section.App
   (SideBar state ch)
   (WorkArea state ch)])

(defn render!
  [state ch]
  (rum/mount (RootUI state ch) (.getElementById js/document "app")))
