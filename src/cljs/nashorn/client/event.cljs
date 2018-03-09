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

(defmethod mutate! :docs/focus
  [state _ msg]
  (assoc state :docs/focus (:doc msg)))

(defmethod mutate! :docs/unfocus
  [state _ _]
  (assoc state :docs/focus nil))

(defmethod mutate! :props/delete
  [state ch msg]
  (http/send! ch {:event :props/delete :key (:key msg)})
  state)

(defmethod mutate! :props/done
  [state _ _]
  (assoc state :view :view/props-home :props/focus nil :script/focus nil))

(defmethod mutate! :props/edit
  [state ch msg]
  (http/send! ch {:event :script/docs})
  (assoc state :view :view/props-edit :props/focus (:key msg)))

(defmethod mutate! :props/focus
  [state _ msg]
  (assoc state :view :view/props-home :props/focus (:key msg) :script/focus nil))

(defmethod mutate! :props/home
  [state ch msg]
  (http/send! ch {:event :props/list})
  (assoc state :view :view/props-home :props/focus nil))

(defmethod mutate! :props/new
  [state ch _]
  (http/send! ch {:event :script/docs})
  (assoc state :view :view/props-new :props/focus nil))

(defmethod mutate! :props/save
  [state ch msg]
  (http/send! ch msg)
  state)

(defmethod mutate! :props/unfocus
  [state _ msg]
  (assoc state :props/focus nil))

(defmethod mutate! :script/delete
  [state ch msg]
  (http/send! ch {:event :script/delete :id (:id msg)})
  state)

(defmethod mutate! :script/done
  [state ch msg]
  (http/send! ch {:event :script/list})
  (assoc state :view :view/home :script/test-result nil :docs/focus nil))

(defmethod mutate! :script/done-results
  [state _ msg]
  (assoc state :script/test-result nil))

(defmethod mutate! :script/edit
  [state ch msg]
  (when (empty? (:docs/list state))
    (http/send! ch {:event :script/docs}))
  (assoc state :script/focus (:id msg) :view :view/edit-script))

(defmethod mutate! :script/focus
  [state ch msg]
  (http/send! ch {:event :script/logs :id (:id msg)})
  (assoc state :view :view/script-home
         :props/focus nil
         :script/focus (:id msg)
         :script/test-result nil))

(defmethod mutate! :script/home
  [state _ msg]
  (assoc state :script/focus nil :view :view/home))

(defmethod mutate! :script/import
  [state ch msg]
  (http/send! ch {:event :script/import :file (:file msg)})
  state)

(defmethod mutate! :script/new
  [state ch msg]
  (when (empty? (:docs/list state))
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
  (assoc state :script/focus nil :script/test-result nil :script/logs []))

(defmethod mutate! :script/update
  [state ch {:keys [event script]}]
  (http/send! ch {:event :script/update :script script})
  state)

(defmethod mutate! :error/dismiss
  [state _ _]
  (assoc state :server/error nil))

;; server responses

(defmethod mutate! :server/import-complete
  [state _ _]
  (assoc state :props/focus nil :script/focus nil))

(defmethod mutate! :server/docs
  [state _ data]
  (assoc state :docs/list (:docs data)))

(defmethod mutate! :server/props-list
  [state _ msg]
  (assoc state :props/list (:properties msg)))

(defmethod mutate! :server/prop-saved
  [state ch msg]
  (mutate! state ch {:event :props/focus :key (:key (:saved msg))}))

(defmethod mutate! :server/prop-deleted
  [state ch msg]
  (mutate! state ch {:event :props/unfocus}))

(defmethod mutate! :server/error
  [state _ data]
  (println "SERVER.ERROR:" (pr-str data))
  (assoc state :server/error data))

(defmethod mutate! :server/test-result
  [state _ data]
  (assoc state :script/test-result (dissoc data :event)))

(defmethod mutate! :server/script-save
  [state ch msg]
  (assoc state :view :view/home :script/test-result nil :script/focus (:id (:saved msg))))

(defmethod mutate! :server/script-logs
  [state ch msg]
  (assoc state :script/logs (:logs msg)))

(defmethod mutate! :server/script-update
  [state ch _]
  (assoc state :script/test-result nil))

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
