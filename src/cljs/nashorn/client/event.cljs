(ns nashorn.client.event
  (:require
   [cljs.core.async :refer [<! go-loop put!]]
   [clojure.string :as string]))

(enable-console-print!)

(defmulti mutate!
  (fn [state msg]
    (:event msg)))

(defmethod mutate! :default
  [state msg]
  (println "Unhandled:" (pr-str msg))
  state)

(defmethod mutate! :script/new
  [state msg]
  (assoc state :view :view/new-script))

(defmethod mutate! :script/done
  [state msg]
  (assoc state :view :view/all-scripts))

(defn loop!
  [ch f]
  (go-loop []
    (when-let [msg (<! ch)]
      (try
        (f msg)
        (catch js/Error e
          (println "ERROR:" (pr-str {:error e :msg msg}))))
      (recur))))
