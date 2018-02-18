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
  (println "Unable to process message:" (pr-str msg))
  state)

(defmethod mutate! :whimsy/do
  [state msg]
  (assoc state :title (str (:title state) "!")))

(defmethod mutate! :whimsy/undo
  [state msg]
  (assoc state :title (string/replace (:title state) "!" "")))

(defn loop!
  [ch f]
  (go-loop []
    (when-let [msg (<! ch)]
      (try
        (f msg)
        (catch js/Error e
          (println "ERROR:" (pr-str {:error e :msg msg}))))
      (recur))))
