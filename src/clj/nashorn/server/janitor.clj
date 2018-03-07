(ns nashorn.server.janitor
  (:require
   [nashorn.server.db :as db]
   [nashorn.server.logging :as log]
   [nashorn.server.thread :as thread]))

(defn- schedule
  [delay duration f]
  (thread/fixed-delay-timer "janitor" delay duration :seconds f))

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
  [{:keys [db limit] :as config}]
  (let [duration (* 5 60) ;; five minutes
        delay 10
        cleaner (schedule delay duration #(clean-logs db limit))]
    {:cleaner cleaner}))

(defn stop!
  [svc]
  (when-let [cleaner (:cleaner svc)]
    (thread/cancel cleaner))
  nil)
