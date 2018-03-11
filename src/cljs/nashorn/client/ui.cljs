(ns nashorn.client.ui
  (:require-macros nashorn.client.ui)
  (:require
   [clojure.string :refer [blank?]]
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
    (do-send! ch event msg)
    (.stopPropagation e)))

(def PureMixin
  rum/static)

(defn- flat1
  [lst]
  (reduce (fn [a v]
            (if (coll? v)
              (apply conj a v)
              (conj a v))) [] lst))

(defn Container ;; Probably the same as react's Fragment
  [name & items]
  (let [el (keyword (str "section." name))
        props? (map? (first items))
        props (if props? (first items) {})
        body (if props? (rest items) items)]
    (vec (concat [el props] (flat1 body)))))

(def ^:private button-icons
  {:close    (icon/Close)
   :download (icon/Download)
   :delete   (icon/Delete)
   :edit     (icon/Edit)
   :new      (icon/New)
   :play     (icon/Play)
   :refresh  (icon/Refresh)
   :run      (icon/Run)
   :save     (icon/Save)
   :stop     (icon/Stop)})

(defc Button < PureMixin {:key-fn #(:label %)}
  [{:keys [type onClick label disabled?] :or {disabled? false}}]
  (let [class (if disabled? ["Button" "Disabled"] ["Button" "Enabled"])
        handler (if disabled? nil onClick)
        props (cond-> {:class class}
                (not disabled?) (assoc :onClick onClick))]
    [:div props
     [:div.Icon (button-icons type)]
     (when-not (blank? label)
       [:div.Label label])]))

(defc Field < PureMixin
  [{:keys [title onChange] :as props}]
  [:div.Field
   [:div.Title (or title "")]
   [:input (assoc props :onChange #(onChange (.-value (.-target %))))]])

(defc Select < PureMixin
  [{:keys [title onChange value] :as props} options]
  [:div.Field
   [:div.Title (or title "")]
   [:select {:value (:value props)
             :onChange #(onChange (.-value (.-target %)))}
    (for [[k v] options]
      [:option {:key k :value k} v])]])

(defc FormHelp < PureMixin
  [{:keys [alert? message]}]
  (let [class (if alert? ["Helper" "Alert"] "Helper")]
    [:div.Field.Help
     [:div {:class class} (str message)]]))

(defc MultiLineField < PureMixin
  {:did-mount (fn [state]
                (let [{:keys [id value]} (first (:rum/args state))
                      node (rum/ref-node state id)]
                  (set! (.-innerText node) value)
                  state))}
  [{:keys [title id onChange value]}]
  [:div.Field
   [:div.Title (or title "")]
   [:div.TextArea {:ref id
                   :onInput #(onChange (.-innerText (.-target %)))
                   :contentEditable "true"}]])
