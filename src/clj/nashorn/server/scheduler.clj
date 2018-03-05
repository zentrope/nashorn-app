(ns nashorn.server.scheduler
  (:refer-clojure :exclude [compile])
  (:require
   [nashorn.lib.cron :as cron])
  (:import
   (java.time LocalDateTime)))

(defn- range-for
  "Return the range of values for a given tab field."
  [field]
  (-> (case field
        :minute (range 0 60)
        :hour (range 0 24)
        :month (range 1 13)
        :date (range 1 32)
        :day (range 1 8)
        :task [:noop]
        nil)
    (vec)))

(def ^:private default-tab
  "By default, a tab will run every minute."
  (let [ks [:minute :hour :month :date :day :task]]
    (into {} (map #(vector % (range-for %)) ks))))

(defn- remove-wildcards
  [tab]
  (reduce-kv #(if (= %3 ["*"]) %1 (assoc %1 %2 %3)) {} tab))

(defn- expand
  "Expand a declarative 'tab' representation into actual data."
  [tab]
  (->> (select-keys tab (keys default-tab))
       remove-wildcards
       (merge default-tab)))

(defn- ticker
  "Return a 'tab' representing the 'time'."
  [^LocalDateTime time]
  {:minute (.getMinute time)
   :hour (.getHour time)
   :day (.getValue (.getDayOfWeek time))
   :month (.getMonthValue time)
   :date  (.getDayOfMonth time)})

(defn- match?
  "True if the 'tick' tab falls within the ranges in 'tab'."
  [tick tab]
  (empty? (filter #(not (contains? (set (% tab)) (% tick))) (keys tick))))

;;-----------------------------------------------------------------------------
;; Public

(defn tasks-to-run
  "Return a list of the tasks to be run at the given time."
  [crontab time]
  (let [tick (ticker time)]
    (apply concat (map :task (filter #(match? tick %) crontab)))))

(defn valid?
  [cron-text]
  (try
    (let [result (cron/parse cron-text)]
      {:valid? true :result result})
    (catch Throwable t
      {:valid? false :error (str t)})))

;; [{:cron "15-45/5 * * * *" :task [:some-id]}].
(defn compile
  [specs]
  (->> specs
       (map #(assoc (cron/parse (:cron %)) :task (:task %)))
       (map expand)))
