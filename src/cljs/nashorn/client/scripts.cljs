(ns nashorn.client.scripts
  (:require
   [nashorn.client.ui :refer [send! PureMixin WorkArea Table]]
   [nashorn.client.icon :as icon]
   [rum.core :as rum :refer [defc]])
  (:import
   [goog.i18n DateTimeFormat]))

(def ^:private iso-ish
  (DateTimeFormat. "yyyy-MM-dd HH:mm"))

(defn- datef
  [s]
  (if (nil? s) "-" (.format iso-ish s)))

(defc Toggle < PureMixin
  [{:keys [id status] :as row} ch]
  (let [new-status (if (zero? status) "inactive" "active")
        msg {:id id :status new-status}]
    [:div.Toggle {:title (str "Click to set to '" new-status "'." )
                  :onClick (send! ch :script/status msg)}
     (if (zero? status)
       (icon/ToggleOn)
       (icon/ToggleOff))]))

(defc ScriptTable < PureMixin
  [{:keys [rows] :as attrs} ch]
  [:div.Lister
   (Table
    ["" "Updated" "Last run" "Extension" "Schedule" ""]
    (for [row rows]
      [:tr
       [:td {:width "2%"}  ""]
       [:td {:width "15%"} (datef (:updated row))]
       [:td {:width "15%"} (datef (:last_run row))]
       [:td {:width "50%"} (:name row)]
       [:td {:width "10%"} (:crontab row)]
       [:td {:width "5%"}  (Toggle row ch)]]))])

(defc Scripts < PureMixin
  [scripts ch]
  (WorkArea
   [:h1 {:style {:marginTop "50px"}} "Current inventory"]
   (ScriptTable {:rows (sort-by :name scripts)} ch)))
