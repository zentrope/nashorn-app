(ns nashorn.client.app
  (:require
   [cljs.core.async :refer [put!]]
   [clojure.walk :refer [postwalk]]
   [rum.core :as rum :refer [defc]]))

;; component helpers

(defn- send!
  [ch event & [msg]]
  (fn [e]
    (let [msg (if (nil? msg) {} msg)]
      (put! ch (merge {:event event} msg)))))

(defn- clean
  [data]
  (postwalk #(if (fn? %) :clean/fn %) data))

(def ^:private PureMixin
  {:should-update (fn [old new]
                    (not= (-> old :rum/args clean)
                          (-> new :rum/args clean)))})

(defc Button < PureMixin
  [{:keys [onClick label]}]
  [:div.Button {:onClick onClick} label])

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
     [:div.Title "Functions"]
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
    (Button {:onClick (send! ch :script/new) :label "New Script"})]
   sb-footer])

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
