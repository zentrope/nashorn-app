(ns nashorn.client.scripts
  (:require
   [clojure.string :refer [replace blank? lower-case]]
   [nashorn.client.codeview :refer [CodeView]]
   [nashorn.client.run-result :refer [ResultPanel]]
   [nashorn.client.ui :refer
    [do-send! send! Container ControlBar DisplayBlock PureMixin WorkArea Table Button IncludeIf]]
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

(defn- manually-run?
  [{:keys [crontab] :as script}]
  (blank? (replace crontab #"\s|[*]" "")))

(defn- status-badge
  [{:keys [status] :as script}]
  (let [class ["Status" (if (zero? status) "Active" "Inactive")]]
    [:div {:class class}
     (icon/Bullet)]))

(defn- schedule-badge
  [script]
  (if (manually-run? script) "" (icon/Calendar)))

(defc ScriptTable < PureMixin
  [{:keys [rows selected?] :as attrs} ch]
  (DisplayBlock {:title "Extensions" :commands []}
    [:div.Lister
     (Table ["Name" "Language" "Updated" "Last run" ""]
       (for [row rows]
         [:tr {:class ["Clickable" (if (selected? row) "Selected")]
               :onClick (send! ch :script/focus {:id (:id row)})}
          [:td {:width "32%"} (status-badge row) (:name row)]
          [:td {:width "16%"} (lower-case (:language row))]
          [:td {:width "25%"} (datef (:updated row))]
          [:td {:width "25%"} (datef (:last_run row))]
          [:td {:width "2%"} (schedule-badge row)]]))]))

(defc Controls
  [{:keys [id status] :as script} ch]
  (let [active? (zero? status)
        focussed? (not (nil? script))]
    (ControlBar
      (IncludeIf
        focussed? (Button {:label "Done"
                           :type :close
                           :onClick (send! ch :script/unfocus)})
        true      (Button {:label "New"
                           :type :new
                           :onClick (send! ch :script/new)})
        focussed? (Button {:label "Run"
                           :type :run
                           :onClick (send! ch :script/run script)})
        focussed? (Button {:label (if active? "Deactivate" "Activate")
                           :type (if active? :stop :play)
                           :onClick (send! ch :script/status {:id id :status (if active? "inactive" "active")})})
        focussed? (Button {:label "Delete"
                           :type :delete
                           :disabled? active?
                           :onClick (confirm-delete script ch)})
        focussed? (Button {:label "Edit"
                           :type :edit
                           :disabled? active?
                           :onClick (send! ch :script/edit {:id id})})
        focussed? (Button {:label "Refresh"
                           :type :refresh
                           :onClick (send! ch :script/focus {:id id})})))))

;; (defc DetailView < PureMixin
;;   [{:keys [id status created updated last_run crontab name] :as script} ch]
;;   (DisplayBlock {:title name :commands []}
;;     [:table.Detailer
;;      [:tbody
;;       [:tr [:th "Created"]  [:td (datef created)]]
;;       [:tr [:th "Updated"]  [:td (datef updated)]]
;;       [:tr [:th "Last run"] [:td (datef last_run)]]
;;       [:tr [:th "Schedule"] [:td (if (manually-run? script) "Run manually" crontab)]]
;;       [:tr [:th "Language"] [:td (:language script)]]
;;       [:tr [:th "Status"]   [:td (if (zero? status) "Active" "Inactive")]]]]))

(defc RunLogsView < PureMixin
  [logs]
  (DisplayBlock {:title "Recent runs" :commands []}
    (if (empty? logs)
      [:p "No run logs available."]
      [:div.Lister
       (Table ["Run" "Result" "Captured" "Error"]
         (for [log logs]
           [:tr
            [:td {:width "25%"} (datef (:created log))]
            [:td {:width "25%"} (:result log)]
            [:td {:width "25%"} (:output log)]
            [:td {:width "25%"} (:error log)]]))])))

(defc SummaryView < PureMixin
  [scripts focus ch]
  (ScriptTable {:rows scripts
                :selected? #(= (:id %) (:id focus))} ch))

(defc Scripts < PureMixin
  [scripts focus run-result logs ch]
  (let [focussed? (not (nil? focus))
        just-ran? (and run-result focussed?)]
    (WorkArea
     (Container "ScriptArea"
         (SummaryView (sort-by :name scripts) focus ch)
         (IncludeIf
           focussed? (CodeView (:script focus) (:language focus))
           ;; focussed? (DetailView focus ch)
           just-ran? (ResultPanel run-result ch)
           focussed? (RunLogsView (reverse (sort-by :created logs)))))
     (Controls focus ch))))
