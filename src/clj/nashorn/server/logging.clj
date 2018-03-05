(ns nashorn.server.logging
  ;;
  ;; Unbuffered, serialized console logging to avoid deps.
  ;;
  (:require
   [clojure.core.async :refer [go]]
   [clojure.string :as string])
  (:import
   (java.text SimpleDateFormat)))

(def ^:private datef
  (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss,SSS"))

(def ^:private fmt
  "%s | extm | %-17s | %-5s %-24s | %s")

(def ^:private levels
  {:info "INFO"
   :warn "WARN"
   :error "ERROR"})

(defn- thread-name
  []
  (.getName (Thread/currentThread)))

(defn- iso-date
  []
  (.format datef (java.util.Date.)))

(defn- gather
  [args]
  (string/join " " args))

(defonce ^:private LOCK (Object.))

(defn log
  [ns l & args]
  (let [tn (thread-name)]
    (go (locking LOCK
          (let [msg (format fmt (iso-date) tn (levels l) ns (gather args))]
            (println msg)
            (flush))))))

(defn logf
  [ns level pattern & args]
  (log ns level (apply format pattern args)))

(defmacro info
  [& args]
  `(log ~*ns* :info ~@args))

(defmacro infof
  [pattern & args]
  `(logf ~*ns* :info ~pattern ~@args))

(defmacro error
  [& args]
  `(log ~*ns* :error ~@args))

(defmacro errorf
  [pattern & args]
  `(logf ~*ns* :error ~pattern ~@args))

(defmacro warn
  [& args]
  `(log ~*ns* :error ~@args))

(defmacro warnf
  [pattern & args]
  `(logf ~*ns* :error ~pattern ~@args))
