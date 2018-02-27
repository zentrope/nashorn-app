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

(defn- run-fetch
  [ch url config]
  (-> (js/fetch url config)
      (.then  #(.text %))
      (.then  #(read-string %))
      (.then  #(put! ch %))
      (.catch #(put! ch {:event :server/error :error %}))))

(defn- post
  [ch path msg]
  (run-fetch ch (urlify path) (postify msg)))

(defn send!
  [ch msg]
  (post ch "dispatch" msg))
