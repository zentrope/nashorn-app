(ns nashorn.main
  (:require
   [clojure.stacktrace :refer [print-stack-trace]]
   [integrant.core :as ig]
   [nashorn.logging :as log]
   [nashorn.web :as web]))

(def config
  {:svc/web {:port 2018}})

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

(defmethod ig/init-key :svc/web
  [_ {:keys [port] :as config}]
  (log/info "Starting web service.")
  (web/start! {:port port}))

(defmethod ig/halt-key! :svc/web
  [_ svc]
  (log/info "Stopping web service.")
  (web/stop! svc))

(defn -main
  [& args]
  (log/info "Starting applicaiton.")
  (hook-uncaught-exceptions! #'catch-uncaughts)
  (let [system (ig/init config)
        lock (promise)]
    (hook-shutdown! #(do (log/info "Stopping application.")
                         (ig/halt! system)
                         (deliver lock :release)))
    (deref lock)
    (log/info "Halt.")
    (System/exit 0)))
