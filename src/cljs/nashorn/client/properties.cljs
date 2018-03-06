(ns nashorn.client.properties
  (:require
   [clojure.string :refer [blank? trim]]
   [nashorn.client.ui :refer [do-send! send! Button ControlBar
                              DisplayBlock Form Field IncludeIf MultiLineField
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

(defc Widgets < PureMixin
  [form {:keys [onKeyChange onValChange]} prop edit? ch]
  (DisplayBlock {:title (if edit? "Property editor" "Set a property")
                 :commands []}
    (Form
     (Field
      {:title "Property name"
       :type "text"
       :autoFocus "true"
       :value (:key form)
       :onChange onKeyChange})
     (MultiLineField
      {:title "Property value"
       :id "pvalue"
       :value (:value form)
       :onChange onValChange}))))

(defc VarTable < PureMixin
  [properties focus ch]
  (DisplayBlock {:title "Properties" :commands []}
    [:div.Lister
     (Table ["Name" "Value"]
       (for [{:keys [key value]} (sort-by :key properties)]
         [:tr {:class ["Clickable" (if (= focus key) "Selected")]
               :onClick (send! ch :props/focus {:key key})}
          [:td key]
          [:td value]]))]))

(defn- update!
  [form key]
  (fn [v]
    (swap! form assoc key (trim v))))

(defn- confirm-delete
  [ch focus]
  (when (js/confirm (str "Delete the '" focus "' property?"))
    (do-send! ch :props/delete {:key focus})))

(defcs PropsForm < PureMixin FormMixin WillMountMixin
  [locals property edit? ch]
  (let [form (:this/form locals)]
    (WorkArea
     (Widgets @form {:onKeyChange (update! form :key)
                     :onValChange (update! form :value)} edit? ch)
     (ControlBar
      (Button {:label "Done"
               :type :close
               :onClick (send! ch :props/done)})
      (Button {:type :save
               :disabled? (not (saveable? @form))
               :label (if edit? "Update" "Save")
               :onClick (send! ch :props/save {:property @form})})))))

(defc NewProp < PureMixin
  [property ch]
  (PropsForm property false ch))

(defc EditProp < PureMixin
  [property ch]
  (PropsForm (assoc property :old-key (:key property)) true ch))

(defc Properties < PureMixin
  [properties focus ch]
  (WorkArea
   (VarTable properties focus ch)
   (ControlBar
     (IncludeIf
       focus (Button {:label "Done"
                      :type :close
                      :onClick (send! ch :props/unfocus)})
       true  (Button {:label "New"
                      :type :new
                      :onClick (send! ch :props/new)})
       focus (Button {:label "Edit"
                      :type :edit
                      :onClick (send! ch :props/edit {:key focus})})
       focus (Button {:label "Delete"
                      :type :delete
                      :onClick #(confirm-delete ch focus)})))))
