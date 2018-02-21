(ns nashorn.web
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [nashorn.logging :as log]
   [nashorn.script :as script]
   [nashorn.webhacks :as webhacks]
   [org.httpkit.server :as httpd]))

(defn- rlog
  [request]
  (log/info (webhacks/request-str request)))

(defn home
  [request]
  (rlog request)
  (webhacks/raw-file request "public/index.html"))

(defn documentation
  [request]
  (rlog request)
  {:status 200
   :headers {"content-type" "application/json"}
   :body (webhacks/encode {:event "server/docs"
                           :docs (edn/read-string (slurp (io/resource "documentation.edn")))})})

(defn script-test
  [request]
  (rlog request)
  (let [text (webhacks/decode (slurp (:body request)))
        run-result (script/eval-script text)]
    {:status 200
     :body (webhacks/encode (assoc run-result :event "server/test-result"))}))

(defn routes
  []
  (fn [request]
    (-> (case (:uri request)
          "/"     (home request)
          "/test" (script-test request)
          "/docs" (documentation request)
          (webhacks/resource request))
        (assoc-in [:headers "Cache-Control"] "no-cache"))))

(defn start!
  [config]
  {:server (httpd/run-server (routes) {:port (:port config) :worker-name-prefix "http-"})})

(defn stop!
  [this]
  (when-let [s (:server this)]
    (s)))
