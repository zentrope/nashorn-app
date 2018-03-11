(ns nashorn.server.script
  (:require
   [clojure.string :as string :refer [lower-case]]
   [nashorn.server.logging :as log])
  (:import
   (org.python.core Options)
   (java.io StringWriter)
   (jdk.nashorn.api.scripting ClassFilter NashornScriptEngineFactory)
   (javax.script ScriptEngineManager)))

(def ^:private legal-pkgs
  ;; Packages available to scripts.
  #{"com.bobo.nashorn.Functions"})

(defn- sandbox
  "Produces a class-loading filter that prevents JS from loading
  classes other than what we allow."
  [whitelist]
  (reify ClassFilter
    (exposeToScripts [this string]
      (contains? whitelist string))))

(defn- class-loader
  []
  ;; This is what the Nashorn code in the JDK does
  (or (.getContextClassLoader (Thread/currentThread))
      (.getClassLoader (class NashornScriptEngineFactory))))

(defn- py-engine
  []
  (set! Options/importSite false)
  (.getEngineByName (ScriptEngineManager.) "jython"))

(defn- js-engine
  []
  (.getScriptEngine (NashornScriptEngineFactory.)
                    (into-array ["--language=es6"])
                    (class-loader)
                    (sandbox legal-pkgs)))

(defn- get-engine
  [type]
  (case (lower-case type)
    "javascript" (js-engine)
    "python"     (py-engine)
    "jython"     (py-engine)
    (throw (IllegalArgumentException. (format "No engine for %s." type)))))

(defn eval-script
  "Runs the evalation, capturing script 'prints' output in a
  string (rather than printing it to stdout)."
  ([^String script ^String language]
   (let [writer (StringWriter.)
         engine (get-engine language)]
     (.setWriter (.getContext engine) writer)
     (try
       (let [result (.eval engine script)]
         {:isError false :result result :output (str writer)})
       (catch Throwable t
         {:isError true :result nil :output (str writer) :error (.getMessage t)}))))
  ([^String script]
   (eval-script script "javascript")))
