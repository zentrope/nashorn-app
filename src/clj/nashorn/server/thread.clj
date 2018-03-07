(ns nashorn.server.thread
  (:import
   (java.util.concurrent ThreadFactory ScheduledThreadPoolExecutor TimeUnit)))

;; My assumption with this code is that using the Executor stuff is
;; somehow better than using the ancient Timer and TimerTask stuff
;; which is far more succinct.

(let [ids (atom {})]
  (defn- gen-id
    [name]
    (swap! ids update-in [name] #(if (nil? %) 1 (inc %)))
    (get @ids name)))

(defn- thread-factory
  [thread-name]
  (proxy [ThreadFactory] []
    (newThread [f]
      (doto (Thread. f)
        (.setName (str thread-name "-" (gen-id thread-name)))))))

(defn- schedule-executor
  [size thread-name]
  (ScheduledThreadPoolExecutor. size (thread-factory thread-name)))

(def ^:private time-units
  {:days         TimeUnit/DAYS
   :hours        TimeUnit/HOURS
   :microseconds TimeUnit/MICROSECONDS
   :milliseconds TimeUnit/MILLISECONDS
   :minutes      TimeUnit/MINUTES
   :nanoseconds  TimeUnit/NANOSECONDS
   :seconds      TimeUnit/SECONDS})

(defn fixed-delay-timer
  [name delay duration unit f]
  (doto (schedule-executor 1 name)
    (.scheduleWithFixedDelay f delay duration (time-units unit))))

(defn fixed-rate-timer
  [name delay duration unit f]
  (doto (schedule-executor 1 name)
    (.scheduleAtFixedRate f delay duration (time-units unit))))

(defn cancel
  [timer]
  (.shutdownNow timer))
