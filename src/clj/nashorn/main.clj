(ns nashorn.main
  (:require
   [clojure.stacktrace :refer [print-stack-trace]]
   [integrant.core :as ig]
   [nashorn.db :as db]
   [nashorn.logging :as log]
   [nashorn.web :as web]))

(def config
  {:svc/web {:port 2018 :db (ig/ref :svc/db)}
   :svc/db  {:spec {:subprotocol "h2"
                    :subname     "file://%s/storage"
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
