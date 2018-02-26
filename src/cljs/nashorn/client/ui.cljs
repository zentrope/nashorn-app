(ns nashorn.client.ui
  (:require-macros
   nashorn.client.ui)
  (:require
   [cljs.core.async :refer [put!]]
   [nashorn.client.icon :as icon]
   [rum.core :as rum :refer [defc]]))

(defn do-send!
  [ch event & [msg]]
  (let [msg (if (nil? msg) {} msg)]
    (put! ch (merge {:event event} msg))))

(defn send!
  [ch event & [msg]]
  (fn [e]
    (do-send! ch event msg)))

(def PureMixin
  rum/static)

(def button-icons
  {:close      (icon/Close)
   :new        (icon/New)
   :play       (icon/Play)
   :run        (icon/Run)
   :save       (icon/Save)
   :stop       (icon/Stop)})

(defc Button < PureMixin
  [{:keys [type onClick label disabled?] :or {disabled? false label "Button"}}]
  (let [class (if disabled? ["Button" "Disabled"] ["Button" "Enabled"])
        handler (if disabled? nil onClick)
        props (cond-> {:class class}
                (not disabled?) (assoc :onClick onClick))]
    [:div props
     [:div.Icon (button-icons type)]
     [:div.Label label]]))
