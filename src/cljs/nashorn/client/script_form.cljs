(ns nashorn.client.script-form
  (:require
   [nashorn.client.ui :refer [send! PureMixin WorkArea]]
   [rum.core :as rum :refer [defc]]))

(defc NewScriptForm < PureMixin
  [ch]
  (WorkArea
   [:h1 "New Script Forms"]
   [:button {:onClick (send! ch :script/done)} "Done"]))
