(ns nashorn.main
  (:require
   [clojure.java.io :as io])
  (:import
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
  (.getScriptEngine (NashornScriptEngineFactory.) (sandbox legal-pkgs)))

(defn- js-eval
  [engine script]
  (.eval engine script))

(def bScript
  (slurp (io/resource "github.js")))

(defn -main
  [& args]
  (let [engine (js-engine)]
    (try
      (js-eval engine bScript)
      (catch Throwable t
        (println "error:" (.getMessage t))))))
