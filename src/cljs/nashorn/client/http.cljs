(ns nashorn.client.http
  (:refer-clojure
   :exclude [get])
  (:require
   [cljs.core.async :refer [put!]]
   [cljs.reader :refer [read-string]]))

(def ^:private headers
  #js {"content-type" "application/edn"})

(defn- urlify
  [path]
  (str js/window.location.href path))

(defn- postify
  [body]
  (clj->js {:method  "POST"
            :headers headers
            :body    (pr-str body)}))

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
      (.then #(.text %))
      (.then #(read-string %))
      (.then #(put! ch %))
      ;;
      (.catch #(put! ch {:event :server/error :error %}))))

(defn- post
  [ch path msg]
  (run-fetch ch (urlify path) (postify msg)))

(defn mutate
  [ch msg]
  (post ch "mutate" msg))

(defn query
  [ch msg]
  (post ch "query" msg))
