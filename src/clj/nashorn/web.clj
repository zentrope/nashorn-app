(ns nashorn.web
  (:require
   [nashorn.logging :as log]
   [nashorn.script :as script]
   [nashorn.webhacks :as webhacks]
   [org.httpkit.server :as httpd]))

(defn home
  [request]
  (webhacks/raw-file request "public/index.html"))

(defn script-test
  [request]
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
          (webhacks/resource request))
        (assoc-in [:headers "Cache-Control"] "no-cache"))))

(defn start!
  [config]
  {:server (httpd/run-server (routes) {:port (:port config) :worker-name-prefix "http-"})})

(defn stop!
  [this]
  (when-let [s (:server this)]
    (s)))
