(ns nashorn.web
  (:require
   [nashorn.webhacks :as webhacks]
   [org.httpkit.server :as httpd]))

(defn home
  [request]
  (webhacks/raw-file request "public/index.html"))

(defn routes
  []
  (fn [request]
    (-> (case (:uri request)
          "/" (home request)
          (webhacks/resource request))
        (assoc-in [:headers "Cache-Control"] "no-cache"))))

(defn start! [{:keys [port] :as config}]
  (let [app (routes)
        params {:port port :worker-name-prefix "http-"}
        server (httpd/run-server app params)]
    {:server server}))

(defn stop!
  [{:keys [server] :as state}]
  (when server
    (server)))
