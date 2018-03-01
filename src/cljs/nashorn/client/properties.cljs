(ns nashorn.client.properties
  (:require
   [clojure.string :refer [blank? trim]]
   [nashorn.client.ui
    :refer [send! Button ControlBar DisplayBlock Form
            Field MultiLineField
            PureMixin Table WorkArea]]
   [rum.core :as rum :refer [defc defcs]]))

(defn- saveable?
  [{:keys [key value] :as form}]
  (and (not (blank? key))
       (not (blank? value))))

(def FormMixin
  (rum/local {} :this/form))

(def WillMountMixin
  {:will-mount (fn [state]
                 (let [[prop _] (:rum/args state)]
                   (reset! (:this/form state) (if prop prop {:key "" :value ""})))
                 state)})

(defcs PropertyForm < PureMixin FormMixin WillMountMixin
  [locals prop ch]
  (let [form (:this/form locals)
        editing? (:updated @form)
        title (if editing? "Property editor" "Set a property")]
    (DisplayBlock
     {:title title
      :commands [(Button {:type :save
                          :disabled? (not (saveable? @form))
                          :label (if editing? "Update" "Save")
                          :onClick (send! ch :props/save {:property @form})})
                 (Button {:type :close
                          :label "Cancel"
                          :onClick (send! ch :props/done)})]}
     (Form
      (Field
       {:title "Property name"
        :type "text"
        :autoFocus "true"
        :value (:key @form)
        :onChange #(swap! (:this/form locals) assoc :key %)})
      (MultiLineField
       {:title "Property value"
        :id "pvalue"
        :value (:value @form)
        :onChange #(swap! (:this/form locals) assoc :value %)})))))

(defc VarTable < PureMixin
  [properties focus ch]
  [:div.Lister
   (Table
    ["Key" "Value"]
    (for [{:keys [key value]} (sort-by :key properties)]
      [:tr {:class ["Clickable" (if (= focus key) "Selected")]
            :onClick (send! ch :props/focus {:key key})}
       [:td key]
       [:td value]]))])

(defn- find-prop
  [props key]
  (if (nil? key)
    nil
    (reduce #(if (= (:key %2) key) (reduced %2) nil) nil props)))

(defc Properties < PureMixin
  [properties focus ch]
  (let [prop (find-prop properties focus)]
    (WorkArea
     (VarTable properties focus ch)
     (PropertyForm prop ch)
     (ControlBar
      (Button {:label "Done"
               :type :close
               :onClick (send! ch :props/done)})))))
