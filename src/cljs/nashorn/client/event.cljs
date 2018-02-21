(ns nashorn.client.event
  (:require
   [cljs.core.async :refer [<! go-loop put!]]
   [clojure.string :as string]))

(defn- ->path
  [path]
  (str js/window.location.href path))

(defn- ->post
  [body]
  (clj->js {:method  "POST"
            :headers (clj->js {:content-type "application/json"})
            :body    (JSON.stringify (clj->js body))}))

(defn- post
  [ch path msg]
  (-> (js/fetch (->path path) (->post msg))
      (.then (fn [response]
               (.json response)))
      (.then (fn [json]
               (put! ch (js->clj json :keywordize-keys true))))
      (.catch (fn [error]
                (put! ch {:event :server/error :error error})))))

;;---

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
  [state _ msg]
  (assoc state :view :view/new-script))

(defmethod mutate! :script/test
  [state ch msg]
  (post ch "test" (:script msg))
  state)

(defmethod mutate! "server/test-result"
  [state _ data]
  (println "test-result:" (pr-str (dissoc data :event)))
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
