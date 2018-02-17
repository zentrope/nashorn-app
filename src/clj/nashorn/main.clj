(ns nashorn.main
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.java.io :as io])
  (:import
   [java.io StringWriter]
   [jdk.nashorn.api.scripting ClassFilter NashornScriptEngineFactory]
   [javax.script ScriptEngineManager]))

(defn sandbox
  "Produces a class-loading filter that prevents JS from loading
  classes other than what we allow."
  [whitelist]
  (reify ClassFilter
    (exposeToScripts [this string]
      (contains? whitelist string))))

(def legal-pkgs
  ;; Packages available to scripts.
  #{"com.bobo.nashorn.Functions"})

(defn- js-engine
  []
  (.getScriptEngine (NashornScriptEngineFactory.)
                    (into-array ["--language=es6"])
                    (ClassLoader/getSystemClassLoader)
                    (sandbox legal-pkgs)))

(defn- js-eval
  [engine script]
  (.eval engine script))

;; This kind of thing lets users use "print" for output or logging
;; that can be captured for a given run of a script.
(defn- do-eval
  "Runs the evalation, capturing script 'prints' output in a
  string (rather than printing it to stdout)."
  [script]
  (let [writer (StringWriter.)
        engine (js-engine)]
    (.setWriter (.getContext engine) writer)
    (try
      (let [result (js-eval engine script)]
        {:error? false :result result :output (str writer)})
      (catch Throwable t
        {:error? true :result nil :output (str writer) :exception t :error (.getMessage t)}))))

(def bScript
  (slurp (io/resource "github.js")))

(defn -main
  [& args]
  (let [{:keys [error? result output exception error] :as rc} (do-eval bScript)]
    (if error?
      (println "ERROR:" (pr-str error))
      (do (println "result:" result)
          (println (format "output:\n%s" output))))))
