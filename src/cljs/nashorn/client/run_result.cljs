(ns nashorn.client.run-result
  (:require
   [clojure.string :as string :refer [blank?]]
   [nashorn.client.ui :refer [do-send! send! Button Conditional DisplayBlock PureMixin WorkArea ]]
   [rum.core :as rum :refer [defc]]))

(defn- result-block
  [name data error?]
  [:div {:class ["ResultBlock" (if error? "Error")]}
   [:div.Title name]
   [:div.Result
    [:pre data]]])

(defn- on-dismiss
  [ch]
  (Button {:label "Dismiss" :type :close
           :onClick (send! ch :script/done-results)}))

(defc ResultPanel < PureMixin
  [result ch]
  (DisplayBlock {:title "Script run results"
                 :commands [(on-dismiss ch)]}
    [:div.ResultBlocks
     (Conditional (:isError result)
       (result-block "Runtime error" (:error result) true))
     (Conditional (not (blank? (:result result)))
       (result-block "Returned result" (:result result) false))
     (Conditional (not (blank? (:output result)))
       (result-block "Captured stdout" (:output result) false))]))
