(ns nashorn.client.http
  (:refer-clojure :exclude [get])
  (:require
   [cljs.core.async :refer [put!]]))

(def ^:private headers #js {"content-type" "application/json"})

(defn- ->path
  [path]
  (str js/window.location.href path))

(defn- ->post
  [body]
  (clj->js {:method  "POST"
            :headers headers
            :body    (JSON.stringify (clj->js body))}))

(defn- run-fetch
  [ch url config]
  (-> (js/fetch url config)
      (.then (fn [response]
               (.json response)))
      (.then (fn [json]
               (put! ch (js->clj json :keywordize-keys true))))
      (.catch (fn [error]
                (put! ch {:event :server/error :error error})))))

(defn post
  [ch path msg]
  (run-fetch ch (->path path) (->post msg)))

(defn get
  [ch path]
  (run-fetch ch (->path path) {:method "GET" :headers headers}))
