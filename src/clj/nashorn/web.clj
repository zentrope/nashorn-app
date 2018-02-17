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

(defn start!
  [config]
  {:server (httpd/run-server (routes) {:port (:port config) :worker-name-prefix "http-"})})

(defn stop!
  [this]
  (when-let [s (:server this)]
    (s)))
