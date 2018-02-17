(ns nashorn.webhacks
  ;;
  ;; Functions in support of simple web-serving in lieue
  ;; of yet another dependency.
  ;;
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as string]))

(def mime-types
  {".js" "application/javascript"
   ".html" "text/html"
   ".css" "text/css"
   ".svg" "image/svg+xml"
   ".png" "image/png"})

(defn mime
  [path]
  (let [ext (subs path (string/last-index-of path "."))]
    (if-let [type (get mime-types ext)]
      type
      "application/octet-stream")))

(defn not-found
  [request]
  {:status 404 :headers {"content-type" "plain/text"} :body "Not found."})

(defn raw-file
  [request path]
  (if-let [page (io/resource path)]
    {:status 200
     :headers {"content-type" (mime path)}
     :body (if (string/starts-with? (str page) "jar")
             (io/input-stream page)
             (io/as-file (io/resource path)))}
    (not-found request)))

(defn resource
  [{:keys [request-method uri] :as request}]
  (if (= :get request-method)
    (raw-file request (str "public" uri))
    (not-found request)))

(def ^:const byte-array-type
  (type (byte-array 0)))

(defn byte-array?
  [o]
  (= (type o) byte-array-type))

(defn decode
  [data]
  (edn/read-string data))

(defn encode
  [data]
  (pr-str data))
