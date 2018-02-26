(ns nashorn.client.scripts
  (:require
   [clojure.pprint :refer [pprint]]
   [nashorn.client.ui :refer [send! PureMixin WorkArea Table Button]]
   [nashorn.client.icon :as icon]
   [rum.core :as rum :refer [defc defcs]])
  (:import
   [goog.i18n DateTimeFormat]))

(def ^:private iso-ish
  (DateTimeFormat. "yyyy-MM-dd HH:mm"))

(defn- datef
  [s]
  (if (nil? s) "-" (.format iso-ish s)))

(defc Badge < PureMixin
  [status]
  (let [class ["Status" (if (zero? status) "Active" "Inactive")]]
    [:div {:class class}
     (icon/Bullet)]))

(defc ScriptTable < PureMixin
  [{:keys [rows onClick selected?] :as attrs} ch]
  [:div.Lister
   (Table
    ["" "Updated" "Last run" "Extension"]
    (for [row rows]
      [:tr {:class ["Clickable" (if (selected? row) "Selected")]
            :onClick #(onClick row)}
       [:td {:width "2%"} (Badge (:status row))]
       [:td {:width "48%"} (:name row)]
       [:td {:width "25%"} (datef (:updated row))]
       [:td {:width "25%"} (datef (:last_run row))]]))])

(defn- find-focus
  [scripts id]
  (first (filter #(= (:id %) id) scripts)))

(defc EmptyControlBar
  []
  [:div.ControlBar])

(defc ControlBar < PureMixin
  [{:keys [id status] :as script} dismiss ch]
  (let [active? (zero? status)]
    [:div.ControlBar

     (Button {:type :run :label "Run"})
     (if (zero? status)
       (Button {:type :stop
                :label "Deactivate"
                :onClick (send! ch :script/status {:id id :status "inactive"})})
       (Button {:type :play
                :label "Activate"
                :onClick (send! ch :script/status {:id id :status "active"})}))
     (Button {:type :new
              :disabled? active?
              :label "Delete"})
     (Button {:type :new
              :disabled? active?
              :label "Edit"})
     (Button {:type :close
              :label "Done"
              :onClick #(dismiss)})]))

(defc DetailView < PureMixin
  [{:keys [id status created updated last_run crontab name] :as script}]
  [:div.Detailer
   [:h1 name]
   [:table
    [:tbody
     [:tr [:th "Created"] [:td (datef created)]]
     [:tr [:th "Updated"] [:td (datef updated)]]
     [:tr [:th "Last run"] [:td (datef last_run)]]
     [:tr [:th "Schedule"] [:td crontab]]
     [:tr [:th "Status"] [:td (if (zero? status) "Active" "Inactive")]]]]
   ;; [:pre (with-out-str (pprint cur-script))]
   ])

(defcs Scripts < PureMixin
  (rum/local nil :script/focus)
  [locals scripts ch]
  (let [cur-script (find-focus scripts @(:script/focus locals))]
    (WorkArea
     [:section.ScriptArea
      [:h1 "Summary"]
      (ScriptTable {:rows (sort-by :name scripts)
                    :selected? #(= (:id %) (:id cur-script))
                    :onClick #(reset! (:script/focus locals) (:id %))} ch)
      (when-not (nil? cur-script)
        (DetailView cur-script))]
     (if (nil? cur-script)
        (EmptyControlBar)
        (ControlBar cur-script #(reset! (:script/focus locals) nil) ch)))))
