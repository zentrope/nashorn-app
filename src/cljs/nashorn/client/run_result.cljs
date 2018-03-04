(ns nashorn.client.run-result
  (:require
   [clojure.string :as string :refer [blank?]]
   [nashorn.client.ui :refer [do-send! send! Button Container DisplayBlock IncludeIf PureMixin]]
   [rum.core :as rum :refer [defc]]))

(defn- result-block
  [name data error?]
  (println "RESULT BLOCK:" name)
  [:div {:class ["ResultBlock" (if error? "Error")]}
   [:div.Title name]
   [:div.Result
    [:pre data]]])

(defn- on-dismiss
  [ch]
  (Button {:label "Dismiss"
           :type :close
           :onClick (send! ch :script/done-results)}))

(defc ResultPanel < PureMixin
  [{:keys [isError result output error] :as run-result} ch]
  (DisplayBlock {:title "Script run results"
                 :commands [(on-dismiss ch)]}
    (Container "ResultBlocks"
      (IncludeIf
        isError               (result-block "Runtime error" error true)
        (not (blank? result)) (result-block "Returned result" result false)
        (not (blank? output)) (result-block "Captured stdout" output false)))))
