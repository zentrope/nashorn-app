(ns nashorn.webhacks
  ;;
  ;; Functions in support of simple web-serving in lieue of yet
  ;; another dependency.
  ;;
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as string]))

(def mime-types
  {".js"   "application/javascript"
   ".html" "text/html"
   ".css"  "text/css"
   ".svg"  "image/svg+xml"
   ".png"  "image/png"})

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

(defn request-str
  [request]
  (format "%s %s"
          (string/upper-case (name (:request-method request)))
          (:uri request)))

(defn decode
  [data]
  (json/read-str data :key-fn keyword))

(defn encode
  [data]
  (json/write-str data))
