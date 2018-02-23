(ns nashorn.client.http
  (:refer-clojure
   :exclude [get])
  (:require
   [cljs.core.async :refer [put!]]))

(def ^:private headers
  #js {"content-type" "application/json"})

(defn- urlify
  [path]
  (str js/window.location.href path))

(defn- postify
  [body]
  (clj->js {:method  "POST"
            :headers headers
            :body    (JSON.stringify (clj->js body))}))

(defn- check-status
  [response url config]
  (if (not= 200 (.-status response))
    (throw {:status (.-status response)
            :code (.-statusText response)
            :url url
            :method config.method
            :body (or (.text response) "")})
    response))

(defn- run-fetch
  [ch url config]
  (-> (js/fetch url config)
      ;;
      (.then #(check-status % url config))
      (.then #(.json %))
      (.then #(put! ch (js->clj % :keywordize-keys true)))
      ;;
      (.catch #(put! ch {:event :server/error :error %}))))

(defn post
  [ch path msg]
  (run-fetch ch (urlify path) (postify msg)))

(defn get
  [ch path]
  (run-fetch ch (urlify path) {:method "GET" :headers headers}))
