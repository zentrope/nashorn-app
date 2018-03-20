(defproject scripto "1"

  :jar-name
  "scripto-classes.jar"

  :uberjar-name
  "scripto-1.jar"

  :dependencies
  [[org.clojure/clojure "1.10.0-alpha4"]
   [org.clojure/core.async "0.4.474"]
   [integrant "0.7.0-alpha1"]
   [http-kit "2.3.0-beta2"]

   ;; For python-based scripts
   [org.python/jython "2.7.1b3"]
   [org.json/json "20180130"]

   ;; Database stuff
   [org.clojure/java.jdbc "0.7.5"]
   [com.h2database/h2 "1.4.197"]]

  :main ^:skip-aot nashorn.server.stub
  :min-lein-eversion "2.8.1"

  :source-paths ["src/clj" "src/cljc"]
  :java-source-paths ["src/java"]
  :javac-opts ["-Xlint:unchecked"]

  :figwheel {:css-dirs ["resources/public"]}
  :cljsbuild
  {:builds
   [
    ;;
    ;; This build is used for interactive development.
    ;;
    {:id "dev"
     :source-paths ["src/cljs" "src/cljc"]
     :figwheel {:on-jsload "nashorn.client.main/reload"}
     :compiler {:output-to "resources/public/main.js"
                :output-dir "resources/public/out"
                :main "nashorn.client.main"
                :optimizations :none
                :asset-path "out"
                :source-map true
                :source-map-timestamp true}}
    ;;
    ;; This build is for the production (uberjar) client.
    ;;
    {:id "client"
     :source-paths ["src/cljs" "src/cljc"]
     :compiler {:output-to "resources/public/main.js"
                :language-in :ecmascript5
                :optimizations :whitespace
                :main "nashorn.client.main"}}]}
  ;;
  :profiles
  {
   ;; The uberjar AOT compiles just the stub, which loads the rest of
   ;; the app which is packaged as source (in Clojure fashion).
   :uberjar
   {:aot [nashorn.server.stub]}
   ;;
   ;; The `dev` profile is for working with the CLJS->JS client and
   ;; for managing aspects of the project, such as dependency updates.
   :dev
   {:resource-paths
    ^:replace ["dev" "resources"]
    ;;
    :plugins
    [[lein-ancient "0.6.15"]   ;; To track dependency updates
     [lein-cljsbuild "1.1.7"]  ;; To manage clojurescript builds
     [lein-figwheel "0.5.15"]] ;; Dynamic web-client re-load

    :dependencies
    [
     ;; Fancy REPL editor at the command line
     [com.bhauman/rebel-readline "0.1.1"]
     ;;
     ;; Clojure to JS compiler
     [org.clojure/clojurescript "1.10.217"]
     ;;
     ;; A programming language editor.
     [cljsjs/codemirror "5.31.0-0"]
     ;;
     ;; Syntax highlighter
     [cljsjs/highlight "9.12.0-1"]
     ;;
     ;; React wrapper
     [rum "0.11.2" :exclusions [com.cognitect/transit-clj
                                com.cognitect/transit-cljs]]
     ;;
     ;; Helpers for working with the system at the REPL
     [integrant/repl "0.3.1"]]}}

  :auto-clean false

  :clean-targets ^{:protect false}
  ["resources/public/out"
   "resources/public/main.js"
   "figwheel_server.log"
   :target-path])
