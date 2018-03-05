(ns nashorn.server.script
  (:require
   [nashorn.server.logging :as log])
  (:import
   [java.io StringWriter]
   [jdk.nashorn.api.scripting ClassFilter NashornScriptEngineFactory]
   [javax.script ScriptEngineManager]))

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

(defn- js-engine
  []
  (.getScriptEngine (NashornScriptEngineFactory.)
                    (into-array ["--language=es6"])
                    (ClassLoader/getSystemClassLoader) ;; ok?
                    (sandbox legal-pkgs)))

(defn- js-eval
  [engine script]
  (.eval engine script))

(defn eval-script
  "Runs the evalation, capturing script 'prints' output in a
  string (rather than printing it to stdout)."
  [^String script]
  (let [writer (StringWriter.)
        engine (js-engine)]
    (.setWriter (.getContext engine) writer)
    (try
      (let [result (js-eval engine script)]
        {:isError false :result result :output (str writer)})
      (catch Throwable t
        {:isError true :result nil :output (str writer) :error (.getMessage t)}))))

(comment
  (require '[clojure.java.io :as io])
  (def bScript
    (slurp (io/resource "github.js")))
  ;;
  (eval-script bScript)
  ;;
  )
