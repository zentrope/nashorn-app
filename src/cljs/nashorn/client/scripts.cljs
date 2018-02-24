(ns nashorn.client.scripts
  (:require
   [nashorn.client.ui :refer [send! PureMixin WorkArea]]
   [rum.core :as rum :refer [defc]])
  (:import
   [goog.i18n DateTimeFormat]))

(def ^:private iso-ish
  (DateTimeFormat. "yyyy-MM-dd HH:mm"))

(def ^:private date-col?
  #{:updated :last_run :created})

(def ^:private headers
  {:name "Extension" :updated "Updated" :last_run "Last run" :crontab "Schedule" :status "Status"})

(def ^:private width
  {:updated "15%" :last_run "15%" :name "45%" :status "10%" :crontab "15%"})

(defn- when-item
  [col row f]
  (if-let [item (col row)]
    (f item)
    "-"))

(defn- fmt
  [col row]
  (case (if (date-col? col) :date col)
    :status (when-item col row #(if (zero? %) "active" "inactive"))
    :date   (when-item col row #(.format iso-ish %))
    (when-item col row #(identity %))))

(defc Table < PureMixin
  [{:keys [cols rows] :as attrs} ch]
  [:div.Lister
   [:table {:style {:-webkit-user-select "none"}}
    [:thead
     [:tr (for [c cols]
            [:th {:key c :width (width c)} (headers c)])]]
    [:tbody
     (for [row rows]
       [:tr.Clickable {:key (:id row)}
        (for [col cols]
          (let [item (fmt col row)]
            [:td {:key (hash [item (:id row)])} item]))])]]])

(defc Scripts < PureMixin
  [scripts ch]
  (WorkArea
   [:section
    [:h1 {:style {:marginTop "50px"}} "Current inventory"]
    (Table {:cols [:updated :last_run :name :crontab :status]
            :rows (sort-by :name scripts)} ch)]))
