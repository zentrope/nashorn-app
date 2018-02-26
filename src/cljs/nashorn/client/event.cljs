(ns nashorn.client.event
  (:require
   [cljs.core.async :refer [<! go-loop]]
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
  [state ch msg]
  (http/query ch {:event :script/list})
  (assoc state :view :view/home :script/test-result nil))

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

(defmethod mutate! :script/status
  [state ch msg]
  (http/mutate ch msg)
  state)

(defmethod mutate! :script/test
  [state ch msg]
  (http/query ch msg)
  state)

(defmethod mutate! :script/focus
  [state _ msg]
  (assoc state :script/focus (:id msg)))

(defmethod mutate! :script/unfocus
  [state _ msg]
  (assoc state :script/focus nil))

;; server responses

(defmethod mutate! :server/docs
  [state _ data]
  (assoc state :script/docs (:docs data)))

(defmethod mutate! :server/error
  [state _ data]
  (println "ERROR:" (:reason data))
  state)

(defmethod mutate! :server/test-result
  [state _ data]
  (assoc state :script/test-result (dissoc data :event)))

(defmethod mutate! :server/script-save
  [state ch _]
  (http/query ch {:event :script/list})
  (assoc state :view :view/home :script/test-result nil))

(defmethod mutate! :server/script-list
  [state _ msg]
  (assoc state :script/list (:scripts msg)))

(defn loop!
  [ch f]
  (go-loop []
    (when-let [msg (<! ch)]
      (try
        (f msg)
        (catch js/Error e
          (println "ERROR:" (pr-str {:error e :msg msg}))))
      (recur))))
