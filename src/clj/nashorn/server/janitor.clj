(ns nashorn.server.janitor
  (:require
   [nashorn.server.db :as db]
   [nashorn.server.logging :as log])
  (:import
   (java.util Timer TimerTask)))

(defn- make-task
  [delay duration f]
  (let [timer (Timer. "janitor" true)
        task (proxy [TimerTask] [] (run [] (f)))]
    (doto timer
      (.schedule task delay duration))))

(defn- clean-logs
  [db limit]
  (try
    (let [ids (db/script-run-ids db)]
      (doseq [id ids]
        (let [[results] (db/script-truncate-runs db id limit)]
          (when-not (zero? results)
            (log/infof "- Cleaned %s logged run%s for script %s." results
                       (if (> results 1) "s" "")
                       id)))))
    (catch Throwable t
      (log/error (str t)))))

(defn start!
  [{:keys [db] :as config}]
  (let [limit 3
        duration (* 5 60 1000)
        delay (* 10 1000)
        cleaner (make-task delay duration #(clean-logs db limit))]
    {:cleaner cleaner}))

(defn stop!
  [svc]
  (when-let [cleaner (:cleaner svc)]
    (.cancel cleaner))
  nil)
