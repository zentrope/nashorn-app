(ns nashorn.client.event
  (:require
   [cljs.core.async :refer [<! go-loop put!]]
   [clojure.string :as string]
   [nashorn.client.http :as http]))

(defmulti mutate!
  (fn [state ch msg]
    (:event msg)))

(defmethod mutate! :default
  [state _ msg]
  (println "Unhandled:" (pr-str msg))
  state)

(defmethod mutate! :script/done
  [state _ msg]
  (assoc state :view :view/all-scripts :script/test-result nil))

(defmethod mutate! :script/done-results
  [state _ msg]
  (assoc state :script/test-result nil))

(defmethod mutate! :script/new
  [state ch msg]
  (when (empty? (:functions state))
    (http/query ch {:event :script/docs}))
  (assoc state :view :view/new-script))

(defmethod mutate! :script/save
  [state ch msg]
  (http/mutate ch msg)
  state)

(defmethod mutate! :script/test
  [state ch msg]
  (http/query ch msg)
  state)

(defmethod mutate! :server/docs
  [state _ data]
  (assoc state :functions (:docs data)))

(defmethod mutate! :server/error
  [state _ data]
  (println "ERROR:" (:reason data))
  state)

(defmethod mutate! :server/test-result
  [state _ data]
  (assoc state :script/test-result (dissoc data :event)))

(defn loop!
  [ch f]
  (go-loop []
    (when-let [msg (<! ch)]
      (try
        (f msg)
        (catch js/Error e
          (println "ERROR:" (pr-str {:error e :msg msg}))))
      (recur))))
