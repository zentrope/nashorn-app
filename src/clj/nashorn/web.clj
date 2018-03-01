(ns nashorn.web
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [nashorn.db :as db]
   [nashorn.logging :as log]
   [nashorn.script :as script]
   [nashorn.webhacks :as webhacks]
   [org.httpkit.server :as httpd]))

;;-----------------------------------------------------------------------------
;; convenience
;;-----------------------------------------------------------------------------

(defn- rlog
  ([request msg]
   (log/info (webhacks/request-str request) (pr-str msg)))
  ([request]
   (log/info (webhacks/request-str request))))

(defn- response
  [status event msg]
  {:status status
   :headers {"content-type" "application/edn"}
   :body (webhacks/encode (merge {:event event} msg))})

(defn- responses
  "Merge multiple responses into one, with multiple messages in the body."
  [& responses]
  (let [status (first (reverse (sort-by :status responses)))
        body (mapv :body responses)]
    {:status status
     :headers {"content-type" "application/edn"}
     :body (str "[" (apply str body) "]")}))

(def ^:private reasons
  {:no-handler "Unable to process message."
   :db-error   "Database error."})

(defn- error
  ([code msg]
   (error code (or (get reasons code) (name code)) msg))
  ([code reason msg]
   (log/error (pr-str {:code code :reason reason :msg msg}))
   (response 500 :server/error {:code code :reason reason :msg msg})))

;;-----------------------------------------------------------------------------
;; handlers
;;-----------------------------------------------------------------------------

(defmulti handle!
  (fn [db msg]
    (:event msg)))

(defmethod handle! :env/list
  [db _]
  (response 200 :server/env-list {:vars (db/env-vars db)}))

(defmethod handle! :default
  [_ msg]
  (error :no-handler msg))

(defmethod handle! :props/save
  [db msg]
  (let [result (db/env-set db (:property msg))]
    (responses
     (response 200 :server/prop-saved {:saved result})
     (handle! db {:event :env/list}))))

(defmethod handle! :script/delete
  [db msg]
  (db/script-delete db (:id msg))
  (handle! db {:event :script/list}))

(defmethod handle! :script/docs
  [db _]
  (let [docs (-> "documentation.edn" io/resource slurp edn/read-string)]
    (response 200 :server/docs {:docs docs})))

(defmethod handle! :script/list
  [db msg]
  (response 200 :server/script-list {:scripts (db/scripts db)}))

(defmethod handle! :script/run
  [db msg]
  (let [run-result (script/eval-script (:text msg))]
    (db/script-mark-run db {:id (:id msg)})
    (responses (response 200 :server/test-result run-result)
               (handle! db {:event :script/list}))))

(defmethod handle! :script/save
  [db msg]
  (if-let [saved (db/script-save db (:script msg))]
    (responses (response 200 :server/script-save saved)
               (handle! db {:event :script/list}))
    (error :db-error msg)))

(defmethod handle! :script/status
  [db msg]
  (db/script-status db (select-keys msg [:id :status]))
  (handle! db {:event :script/list}))

(defmethod handle! :script/test
  [db msg]
  (let [run-result (script/eval-script (:text msg))]
    (response 200 :server/test-result run-result)))

(defmethod handle! :script/update
  [db msg]
  (if-let [updated (db/script-update db (:script msg))]
    (responses (response 200 :server/script-update updated)
               (handle! db {:event :script/list}))
    (error :db-error msg)))

;;-----------------------------------------------------------------------------
;; endpoints
;;-----------------------------------------------------------------------------

(defn- home
  [request]
  (rlog request)
  (webhacks/raw-file request "public/index.html"))

(defn- dispatch
  [request db]
  (let [msg (-> request :body slurp webhacks/decode)]
    (rlog request msg)
    (try
      (handle! db msg)
      (catch Throwable t
        (error :exception (str t) msg)))))

;;-----------------------------------------------------------------------------
;; service
;;-----------------------------------------------------------------------------

(defn- routes
  [request db]
  (-> (case (:uri request)
        "/"         (home request)
        "/dispatch" (dispatch request db)
        (webhacks/resource request))
      (assoc-in [:headers "Cache-Control"] "no-cache")))

(defn start!
  [config]
  {:server (httpd/run-server #(routes % (:db config))
                             {:port (:port config)
                              :worker-name-prefix "http-"})})

(defn stop!
  [this]
  (when-let [s (:server this)]
    (s)))
