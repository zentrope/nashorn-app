(ns nashorn.server.job
  (:refer-clojure :exclude [replace])
  (:require
   [clojure.core.async :refer [<! chan close! go go-loop put!]]
   [clojure.string :refer [blank? replace]]
   [nashorn.server.db :as db]
   [nashorn.server.logging :as log]
   [nashorn.server.scheduler :as sched]
   [nashorn.server.script :as script]
   [nashorn.server.thread :as thread])
  (:import
   (java.time LocalDateTime)))

(defn- schedule
  [name delay f]
  (thread/fixed-rate-timer name delay 60 :seconds f))

(defn- prune-manuals
  [schedules]
  (filter #(not (blank? (replace (:crontab %) #"\s|[*]" ""))) schedules))

(defn- script->spec
  [script]
  {:task [(:id script)] :cron (:crontab script)})

;; Query the DB every once in a while to stock the store
;; with the most recent set of schedules.

(defn- gather
  [store db]
  (try
    (let [schedules (db/script-schedules db)
          runnable (prune-manuals schedules)]
      (when-not (= runnable @store)
        (log/info "- Database changed, re-caching schedules.")
        (reset! store runnable)))
    (catch Throwable t
      (log/errorf "Unable to query for scripts: %s." (str t)))))

;; The checker periodically compiles a new crontab from the active
;; scripts in the store, evaluates which ones need to run, then
;; enqueues them for the dispatcher.

(defn- checker
  [store db queue]
  (let [specs (mapv script->spec @store)
        crontab (sched/compile specs)]
    (let [tasks (sched/tasks-to-run crontab (LocalDateTime/now))]
      (doseq [task tasks]
        (put! queue task)))))

;; The dispatcher takes jobs from the queue and runs them in a go
;; block.

(defn- dispatch-job
  [db script-id]
  (let [script (db/script-find db script-id)
        desc (select-keys script [:id :name :crontab])]
    (log/infof "- running scheduled task: %s" (pr-str desc))
    (let [result (script/eval-script (:script script))]
      (db/run-save db (assoc result :script-id script-id)))))

(defn- dispatcher
  [db queue]
  (go-loop []
    (when-let [task (<! queue)]
      ;; Dispatch in a go block to keep the queue flowing
      (go (dispatch-job db task))
      (recur))))

;; Bootstrap

(defn start!
  [{:keys [db] :as config}]
  (let [store (atom [])
        queue (chan 100)
        syncher (schedule "job-gather"   5 #(gather store db))
        checker (schedule "job-checker" 15 #(checker store db queue))]
    (dispatcher db queue)
    {:store store :syncher syncher :checker checker :queue queue}))

(defn stop!
  [svc]
  (when-let [q (:queue svc)]
    (close! q))
  (when-let [syncher (:syncher svc)]
    (thread/cancel syncher))
  (when-let [checker (:checker svc)]
    (thread/cancel checker))
  nil)
