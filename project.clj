(defproject nashorn "1"
  :dependencies [[org.clojure/clojure "1.10.0-alpha4"]]
  :main ^:skip-aot nashorn.stub
  :min-lein-eversion "2.8.1"
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-opts ["-Xlint:unchecked"]
  :profiles {:uberjar {:aot [oid.stub]}
             :dev {:plugins [[lein-ancient "0.6.15"]]}})
