(ns nashorn.server.main
  (:require
   [clojure.stacktrace :refer [print-stack-trace]]
   [clojure.string :refer [join]]
   [integrant.core :as ig]
   [nashorn.server.db :as db]
   [nashorn.server.janitor :as janitor]
   [nashorn.server.job :as job]
   [nashorn.server.logging :as log]
   [nashorn.server.web :as web])
  (:import
   (java.io File)))

(def ^:private sep
  File/separator)

(def ^:private app-dir
  (str (System/getProperty "user.home") sep ".kfi" sep "scripto_app"))

(def config
  {:svc/web     {:port 2018 :db (ig/ref :svc/db)}
   :svc/job     {:db (ig/ref :svc/db)}
   :svc/janitor {:db (ig/ref :svc/db) :limit 3}
   :svc/db      {:app-dir app-dir
                 :spec    {:subprotocol "h2"
                           :subname     (str "file://" app-dir sep "storage")
                           :user        "sa"
                           :password    ""}}})

(defn hook-shutdown! [f]
  (doto (Runtime/getRuntime)
    (.addShutdownHook (Thread. f))))

(defn hook-uncaught-exceptions! [f]
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (f thread ex)))))

(defn catch-uncaughts [thread ex]
  (log/errorf "Caught: [%s] on thread [%s]." ex (.getName thread))
  (log/error (with-out-str (print-stack-trace ex))))

;; DB component

(defmethod ig/init-key :svc/db
  [_ config]
  (log/info "Starting db service.")
  (db/start! config))

(defmethod ig/halt-key! :svc/db
  [_ svc]
  (log/info "Stopping db service.")
  (db/stop! svc))

;; Janitor component

(defmethod ig/init-key :svc/janitor
  [_ config]
  (log/info "Starting janitor service.")
  (janitor/start! config))

(defmethod ig/halt-key! :svc/janitor
  [_ svc]
  (log/info "Stopping janitor service.")
  (janitor/stop! svc))

;; Job component

(defmethod ig/init-key :svc/job
  [_ config]
  (log/info "Starting job service.")
  (job/start! config))

(defmethod ig/halt-key! :svc/job
  [_ svc]
  (log/info "Stopping job service.")
  (job/stop! svc))

;; Web component

(defmethod ig/init-key :svc/web
  [_ config]
  (log/info "Starting web service.")
  (web/start! {:port (:port config) :db (:db config)}))

(defmethod ig/halt-key! :svc/web
  [_ svc]
  (log/info "Stopping web service.")
  (web/stop! svc))

;; Bootstrap

(defn -main
  [& args]
  (log/info "Starting application.")
  (log/infof "→ Using java %s." (System/getProperty "java.version"))
  (log/infof "→ Open web-app at http://localhost:%s." (:port (:svc/web config)))
  (hook-uncaught-exceptions! #'catch-uncaughts)
  (let [system (ig/init config)
        lock (promise)]
    (hook-shutdown! #(do (log/info "Stopping application.")
                         (ig/halt! system)
                         (deliver lock :release)))
    (deref lock)
    (log/info "Halt.")
    (System/exit 0)))
