(ns nashorn.client.import-pane
  (:require
   [nashorn.client.ui :refer [do-send! PureMixin]]
   [rum.core :as rum :refer [defc]]))

(defn- drag-over
  [e]
  (.preventDefault e)
  (.stopPropagation e))

(defn- on-drop
  [callback ch]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (callback)
    (when (.-FileReader js/window)
      (let [f (aget (.-files (.-dataTransfer e)) 0)
            rdr (js/FileReader.)]
        (set! (.-onload rdr) (fn [e]
                               (let [source (.-result (.-target e))]
                                 (do-send! ch :script/import {:file source}))))
        (.readAsText rdr f)))))

(defc ImportPane < PureMixin
  [onDrop ch]
  [:div#ImportPane {:onDragOver drag-over
                    :onDragLeave #(onDrop)
                    :onDrop (on-drop onDrop ch)}])
