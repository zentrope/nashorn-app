(ns nashorn.web
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [nashorn.db :as db]
   [nashorn.logging :as log]
   [nashorn.script :as script]
   [nashorn.webhacks :as webhacks]
   [org.httpkit.server :as httpd]))

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

(defn- home
  [request]
  (rlog request)
  (webhacks/raw-file request "public/index.html"))

;;

(defmulti query!
  (fn [db msg]
    (:event msg)))

(defmethod query! :default
  [_ msg]
  (response 500 :server/error {:code :no-handler
                               :reason "Unable to process msg."
                               :msg msg}))

(defmethod query! :script/docs
  [db _]
  (let [docs (-> "documentation.edn" io/resource  slurp edn/read-string)]
    (response 200 :server/docs {:docs docs})))

(defmethod query! :script/test
  [db msg]
  (let [run-result (script/eval-script (:text msg))]
    (response 200 :server/test-result run-result)))

(defmethod query! :extension/list
  [db msg]
  (response 200 :server/extentions {:data (db/extensions db)}))

(defn- query
  [request db]
  (let [msg (-> request :body slurp webhacks/decode)]
    (rlog request msg)
    (query! db msg)))

;;

(defmulti mutate!
  (fn [db msg] (:event msg)))

(defmethod mutate! :default
  [_ msg]
  (response 500 :server/error {:code :no-handler
                               :reason "Unable to process msg."
                               :msg msg}))

(defn- mutate
  [request db]
  (let [msg (-> request :body slurp webhacks/decode)]
    (rlog request msg)
    (response 200 :server/extension-save {})))

;;

(defn- routes
  [request db]
  (-> (case (:uri request)
        "/"           (home request)
        "/mutate"     (mutate request db)
        "/query"      (query request db)
        (webhacks/resource request))
      (assoc-in [:headers "Cache-Control"] "no-cache")))

;;

(defn start!
  [config]
  {:server (httpd/run-server #(routes % (:db config))
                             {:port (:port config)
                              :worker-name-prefix "http-"})})

(defn stop!
  [this]
  (when-let [s (:server this)]
    (s)))
