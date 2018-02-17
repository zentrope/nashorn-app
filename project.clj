(defproject nashorn "1"
  :dependencies [[org.clojure/clojure "1.10.0-alpha4"]
                 [org.clojure/core.async "0.4.474"]
                 [integrant "0.7.0-alpha1"]
                 [http-kit "2.3.0-alpha5"]]
  :main ^:skip-aot nashorn.stub
  :min-lein-eversion "2.8.1"
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-opts ["-Xlint:unchecked"]
  :profiles {:uberjar {:aot [oid.stub]}
             :dev {:plugins [[lein-ancient "0.6.15"]]}})
