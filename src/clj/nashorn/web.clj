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
;; query handlers
;;-----------------------------------------------------------------------------

(defmulti handle!
  (fn [db msg]
    (:event msg)))

(defmethod handle! :default
  [_ msg]
  (error :no-handler msg))

(defmethod handle! :script/delete
  [db msg]
  (db/delete-script db (:id msg))
  (handle! db {:event :script/list}))

(defmethod handle! :script/docs
  [db _]
  (let [docs (-> "documentation.edn" io/resource  slurp edn/read-string)]
    (response 200 :server/docs {:docs docs})))

(defmethod handle! :script/list
  [db msg]
  (response 200 :server/script-list {:scripts (db/scripts db)}))

(defmethod handle! :script/status
  [db msg]
  (db/status db (:id msg) (:status msg))
  (handle! db {:event :script/list}))

(defmethod handle! :script/test
  [db msg]
  (let [run-result (script/eval-script (:text msg))]
    (response 200 :server/test-result run-result)))

(defmethod handle! :script/save
  [db msg]
  (if-let [saved (db/save-script db (:script msg))]
    (response 200 :server/script-save saved)
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
        "/"       (home request)
        "/mutate" (dispatch request db)
        "/query"  (dispatch request db)
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
