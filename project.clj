(defproject nashorn "1"

  :jar-name
  "nashorn-classes.jar"

  :uberjar-name
  "nashorn-1.jar"

  :dependencies
  [[org.clojure/clojure "1.10.0-alpha4"]
   [org.clojure/core.async "0.4.474"]
   [integrant "0.7.0-alpha1"]
   [http-kit "2.3.0-alpha5"]
   [org.clojure/java.jdbc "0.7.5"]
   [com.h2database/h2 "1.4.196"]]

  :main ^:skip-aot nashorn.stub
  :min-lein-eversion "2.8.1"
  :source-paths ["src/clj"]

  :java-source-paths ["src/java"]
  :javac-opts ["-Xlint:unchecked"]

  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "nashorn.client.main/reload"}
                        :compiler {:output-to "resources/public/main.js"
                                   :output-dir "resources/public/out"
                                   :main "nashorn.client.main"
                                   :optimizations :none
                                   :asset-path "out"
                                   :source-map true
                                   :source-map-timestamp true}}
                       {:id "client"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/main.js"
                                   :language-in :ecmascript5
                                   :optimizations :whitespace
                                   :main "nashorn.client.main"}}]}
  ;;
  :profiles {:uberjar {:aot [nashorn.stub]}
             :dev {:resource-paths ^:replace ["dev" "resources"]
                   :plugins [[lein-ancient "0.6.15"]
                             [lein-cljsbuild "1.1.7" :exclusions [org.clojure/clojure]]
                             [lein-figwheel "0.5.14"  :exclusions [org.clojure/clojure]]]
                   :dependencies [[org.clojure/clojurescript "1.9.946"]
                                  [cljsjs/codemirror "5.31.0-0"]
                                  [rum "0.11.2" :exclusions [com.cognitect/transit-clj
                                                             com.cognitect/transit-cljs]]
                                  [integrant/repl "0.3.0"]]}}
  :auto-clean false
  :clean-targets ^{:protect false}
  ["resources/public/out"
   "resources/public/main.js"
   "figwheel_server.log"
   :target-path])
