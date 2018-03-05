(ns nashorn.server.job
  (:refer-clojure :exclude [replace])
  (:require
   [clojure.core.async :refer [<! chan close! go go-loop put!]]
   [clojure.string :refer [blank? replace]]
   [nashorn.server.db :as db]
   [nashorn.server.logging :as log]
   [nashorn.server.scheduler :as sched]
   [nashorn.server.script :as script])
  (:import
   (java.time LocalDateTime)
   (java.util Timer TimerTask)))

(let [id (atom 0)]
  (defn- new-timer
    [name delay f]
    (let [timer (Timer. (str name "-" (swap! id inc)) true)
          task (proxy [TimerTask] [] (run [] (f)))]
      (doto timer
        (.scheduleAtFixedRate task delay (* 60 1000))))))

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
      (log/info "- schedules:" {:runnable (count runnable) :total (count schedules)}
                (pr-str runnable))
      (reset! store runnable))
    (catch Throwable t
      (log/errorf "Unable to query for scripts: %s." (str t)))))

;; The checker periodically compiles a new crontab from the active
;; scripts in the store, evaluates which ones need to run, and
;; enqueues them for the dispatcher to handle.

(defn- checker
  [store db queue]
  (let [specs (mapv script->spec @store)
        crontab (sched/compile specs)]
    (log/info "- specs are:" specs)
    (let [tasks (sched/tasks-to-run crontab (LocalDateTime/now))]
      (doseq [task tasks]
        (put! queue task))
      (log/info "- tasks to run:" (pr-str tasks)))))

(defn- dispatch-job
  [db script-id]
  (let [script (db/script-find db script-id)]
    (log/info "- task to run" (select-keys script [:id :name :crontab]))
    (let [result (script/eval-script (:script script))]
      (log/info "- result:" (pr-str result)))))

(defn- dispatcher
  [db queue]
  (go-loop []
    (when-let [task (<! queue)]
      ;; Dispatch in a go block to keep the queue flowing
      (go (dispatch-job db task))
      (recur))))

(defn start!
  [{:keys [db] :as config}]
  (let [store (atom [])
        queue (chan 100)
        syncher (new-timer "job-gather"  5000 #(gather store db))
        checker (new-timer "job-checker" 8000 #(checker store db queue))]
    (dispatcher db queue)
    {:store store :syncher syncher :checker checker :queue queue}))

(defn stop!
  [svc]
  (when-let [q (:queue svc)]
    (close! q))
  (when-let [syncher (:syncher svc)]
    (.cancel syncher))
  (when-let [checker (:checker svc)]
    (.cancel checker))
  nil)
