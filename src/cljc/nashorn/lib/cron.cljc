(ns nashorn.lib.cron
  (:require
   [clojure.string :refer [blank? split includes? join]])
  #?(:cljs (:import [goog.string format])))

#_(defn- ord-suffix
    [c number]
    (case c
      "1" (str number "st")
      "2" (str number "nd")
      "3" (str number "rd")
      (str number "th")))

#_(defn- ordinalize
    [number] ;; string
    (let [number (str number)
          len (count number)]
      (if (= 1 len)
        (ord-suffix number number)
        (let [sec-last (subs number (- len 2) (dec len))
              lst-last (subs number (dec len))]
          (println sec-last lst-last)
          (if (= sec-last "1")
            (str number "th")
            (ord-suffix lst-last number))))))

(defn- range?
  [v]
  (includes? v "-"))

(defn- step?
  [v]
  (includes? v "/"))

(defn- to-int
  [x]
  #?(:cljs (js/parseInt x 10))
  #?(:clj (Long/parseLong x)))

(defn- tokenize
  [v]
  (let [[range-part step] (split v "/")
        [min max]         (split range-part "-")]
    (when (and (not (= min "*")) (nil? max) (not (blank? step)))
      (throw "a step '/' must be preceded by a range 'x-y'"))
    (filterv (complement nil?) [min max step])))

(defn- parse-val
  [raw min max]
  (if (nil? raw)
    "*"
    (let [[from to step :as tokens] (tokenize raw)]
      (case (count tokens)
        1 [(if (= from "*") from (to-int from))]
        2 (if (= from "*")
            (range min (inc max) (to-int to))
            (range (to-int from) (inc (to-int to))))
        (range (to-int from) (inc (to-int to)) (to-int step))))))

(defn- check!
  [tag values min max]
  (if (= "*" (first values))
    :done
    (if (not (<= min (first values) (last values) max))
      (throw (format "%s values must be between %s and %s (or '*')" tag min max))
      :done)))

(defn- expand-phase!
  [{:keys [min hour day-of-month month day-of-week] :as crontab}]
  {:min (parse-val min 0 59)
   :hour (parse-val hour 0 23)
   :day-of-month (parse-val day-of-month 1 31)
   :month (parse-val month 1 12)
   :day-of-week (parse-val day-of-week 1 7)})

(defn- validate-phase!
  [{:keys [min hour day-of-month month day-of-week] :as crontab}]
  (check! "minute" min 0 59)
  (check! "hour" hour 0 23)
  (check! "day-of-month" day-of-month 1 31)
  (check! "month" month 1 12)
  (check! "day-of-week" day-of-week 1 7)
  crontab)

(defn- parse
  [crontab]
  (let [[min hour dom mo dow] (take 5 (split crontab #"\s"))]
    (-> {:min min :hour hour :day-of-month dom :month mo :day-of-week dow}
        (expand-phase!)
        (validate-phase!))))

(defn describe
  [cron]
  (if (= cron "* * * * *")
    {:error? false :text "Run manually."}
    (try
      (let [{:keys [min hour day-of-month month day-of-week] :as tab} (parse cron)]
        {:error? false :text (pr-str tab)})
      (catch #?(:cljs :default :clj Throwable) e
        {:error? true :text (str e)}))))
