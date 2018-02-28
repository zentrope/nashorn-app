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

(defmethod mutate! :script/delete
  [state ch msg]
  (http/send! ch {:event :script/delete :id (:id msg)})
  state)

(defmethod mutate! :script/done
  [state ch msg]
  (http/send! ch {:event :script/list})
  (assoc state :view :view/home :script/test-result nil))

(defmethod mutate! :script/done-results
  [state _ msg]
  (assoc state :script/test-result nil))

(defmethod mutate! :script/edit
  [state ch msg]
  (assoc state :script/focus (:id msg) :view :view/edit-script))

(defmethod mutate! :script/focus
  [state _ msg]
  (assoc state :script/focus (:id msg) :script/test-result nil))

(defmethod mutate! :script/new
  [state ch msg]
  (when (empty? (:functions state))
    (http/send! ch {:event :script/docs}))
  (assoc state :view :view/new-script :script/test-result nil))

(defmethod mutate! :script/run
  [state ch msg]
  (http/send! ch {:event :script/run :id (:id msg) :text (:script msg)})
  (assoc state :script/test-result nil))

(defmethod mutate! :script/save
  [state ch msg]
  (http/send! ch msg)
  state)

(defmethod mutate! :script/status
  [state ch msg]
  (http/send! ch msg)
  state)

(defmethod mutate! :script/test
  [state ch msg]
  (http/send! ch msg)
  (assoc state :script/test-result nil))

(defmethod mutate! :script/unfocus
  [state _ msg]
  (assoc state :script/focus nil :script/test-result nil))

(defmethod mutate! :script/update
  [state ch {:keys [event script]}]
  (http/send! ch {:event :script/update :script script})
  state)

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
  (assoc state :view :view/home :script/test-result nil))

(defmethod mutate! :server/script-update
  [state ch _]
  (assoc state :view :view/home :script/test-result nil))

(defmethod mutate! :server/script-list
  [state _ msg]
  (assoc state :script/list (:scripts msg)))

(defn loop!
  [ch f]
  (go-loop []
    (when-let [msg (<! ch)]
      (try
        (if (vector? msg)
          (doseq [m msg]
            (f m))
          (f msg))
        (catch js/Error e
          (println "ERROR:" (pr-str {:error e :msg msg}))))
      (recur))))
