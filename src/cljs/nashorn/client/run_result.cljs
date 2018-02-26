(ns nashorn.client.run-result
  (:require
   [clojure.string :as string :refer [blank?]]
   [nashorn.client.ui :refer [do-send! send! PureMixin WorkArea Button DisplayBlock]]
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
                 (when (:isError result)
                   (result-block "Runtime error" (:error result) true))
                 (when-not (blank? (:result result))
                   (result-block "Returned result" (:result result) false))
                 (when-not (blank? (:output result))
                   (result-block "Captured stdout" (:output result) false))]))
