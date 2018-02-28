(ns nashorn.client.scripts
  (:require
   [nashorn.client.run-result :refer [ResultPanel]]
   [nashorn.client.ui :refer
    [do-send! send! ControlBar DisplayBlock PureMixin WorkArea Table Button]]
   [nashorn.client.icon :as icon]
   [rum.core :as rum :refer [defc]])
  (:import
   [goog.i18n DateTimeFormat]))

(def ^:private iso-ish
  (DateTimeFormat. "yyyy-MM-dd HH:mm"))

(defn- datef
  [s]
  (if (nil? s) "-" (.format iso-ish s)))

(defn- confirm-delete
  [{:keys [id name]} ch]
  (fn [_]
    (when (js/confirm (str "Delete the '" name "' extension?"))
      (do-send! ch :script/delete {:id id}))))

(defc Badge < PureMixin
  [status]
  (let [class ["Status" (if (zero? status) "Active" "Inactive")]]
    [:div {:class class}
     (icon/Bullet)]))

(defc ScriptTable < PureMixin
  [{:keys [rows selected?] :as attrs} ch]
  [:div.Lister
   (Table
    ["" "Extension" "Updated" "Last run"]
    (for [row rows]
      [:tr {:class ["Clickable" (if (selected? row) "Selected")]
            :onClick (send! ch :script/focus {:id (:id row)})}
       [:td {:width "2%"} (Badge (:status row))]
       [:td {:width "48%"} (:name row)]
       [:td {:width "25%"} (datef (:updated row))]
       [:td {:width "25%"} (datef (:last_run row))]]))])

(defc Controls
  [{:keys [id status] :as script} ch]
  (if (nil? script)
    (ControlBar)
    (let [active? (zero? status)]
      (ControlBar
       (Button {:type :run
                :label "Run"
                :onClick (send! ch :script/run script)})
       (Button {:type (if active? :stop :play)
                :label (if active? "Deactivate" "Activate")
                :onClick (send! ch :script/status
                                {:id id :status (if active? "inactive" "active")})})
       (Button {:label "Delete"
                :type :delete
                :disabled? active?
                :onClick (confirm-delete script ch)})
       (Button {:label "Edit"
                :type :edit
                :disabled? active?
                :onClick (send! ch :script/edit {:id id})})
       (Button {:type :close
                :label "Unfocus"
                :onClick (send! ch :script/unfocus)})))))

(defc DetailView < PureMixin
  [{:keys [id status created updated last_run crontab name] :as script} ch]
  (DisplayBlock {:title name
                 :commands [(Button {:type :close
                                     :label "Unfocus"
                                     :onClick (send! ch :script/unfocus)})]}
   [:table.Detailer
    [:tbody
     [:tr [:th "Created"] [:td (datef created)]]
     [:tr [:th "Updated"] [:td (datef updated)]]
     [:tr [:th "Last run"] [:td (datef last_run)]]
     [:tr [:th "Schedule"] [:td crontab]]
     [:tr [:th "Status"] [:td (if (zero? status) "Active" "Inactive")]]]]))

(defc SummaryView < PureMixin
  [scripts focus ch]
  (ScriptTable {:rows scripts
                :selected? #(= (:id %) (:id focus))} ch))

(defc Scripts < PureMixin
  [scripts focus run-result ch]
  (WorkArea
   [:section.ScriptArea
    (SummaryView (sort-by :name scripts) focus ch)
    (when-not (nil? focus)
      (DetailView focus ch))
    (when-not (nil? run-result)
      (ResultPanel run-result ch))]
   (Controls focus ch)))
