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
  (let [el (keyword (str "section." name))]
    (vec (cons el (flat1 items)))))

(def ^:private button-icons
  {:close  (icon/Close)
   :delete (icon/Delete)
   :edit   (icon/Edit)
   :new    (icon/New)
   :play   (icon/Play)
   :run    (icon/Run)
   :save   (icon/Save)
   :stop   (icon/Stop)})

(defc Button < PureMixin {:key-fn #(:label %)}
  [{:keys [type onClick label disabled?] :or {disabled? false}}]
  (let [class (if disabled? ["Button" "Disabled"] ["Button" "Enabled"])
        handler (if disabled? nil onClick)
        props (cond-> {:class class}
                (not disabled?) (assoc :onClick onClick))]
    [:div props
     [:div.Icon (button-icons type)]
     (when label
       [:div.Label label])]))

(defc Field < PureMixin
  [{:keys [title onChange] :as props}]
  [:div.Field
   [:div.Title (or title "")]
   [:input (assoc props :onChange #(onChange (.-value (.-target %))))]])

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
