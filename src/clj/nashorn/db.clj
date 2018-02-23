(ns nashorn.db
  (:require
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as string]
   [nashorn.logging :as log]))

(def ^:private sep
  java.io.File/separator)

(def ^:private home
  (System/getProperty "user.home"))

(defn- init-dir!
  [dir]
  (.mkdirs (java.io.File. dir))
  dir)

;; migration infrastructure

(def ^:private migration-table
  (jdbc/create-table-ddl
   "if not exists migration"
   [[:id :serial "primary key"]
    [:name :varchar "not null"]
    [:created_at :timestamp "not null" "default current_timestamp"]]))

(defn- sql-now
  ([]
   (java.sql.Timestamp. (System/currentTimeMillis)))
  ([ms]
   (java.sql.Timestamp. ms)))

(defn- already-run?
  [conn]
  (->> (jdbc/query conn ["select name from migration"])
       (mapv :name)
       (mapv str)
       set)) ;; sets implement a member-of function

(defn- run-and-record!
  [conn migration]
  (let [m-name (-> migration meta :name str)]
    (log/info "→ running db migration:" m-name)
    (migration conn)
    (jdbc/insert! conn :migration {:name m-name :created_at (sql-now)})))

(defn- load-sql!
  [sql-source]
  (let [raw (slurp (io/resource sql-source))
        smooth-fn (fn [sql]
                    (-> sql
                        (string/replace #"\n" " ")
                        (string/replace #"\s+" " ")
                        string/trim))
        ;; Remove comments
        lines (->> (string/split raw #"\n")
                   ;; broken if embedded comments
                   (filterv #(not (.startsWith % "--")))
                   (string/join #" "))]
    (mapv smooth-fn (string/split lines #";"))))

(defn- load-and-invoke!
  [conn schema-file]
  (let [schema (load-sql! schema-file)]
    (jdbc/db-do-commands conn schema)))

(defn- migrate!
  [pool & migrations]
  (jdbc/with-db-connection [conn pool]
    (jdbc/db-do-commands conn migration-table)
    (let [accomplished? (already-run? conn)]
      (doseq [m migrations]
        (when-not (accomplished? (-> m meta :name str))
          (run-and-record! conn m))))))

;;-----------------------------------------------------------------------------
;; Migrations
;;-----------------------------------------------------------------------------

(defn- mig-001
  [conn]
  (load-and-invoke! conn "sql/mig-001.sql"))

;;-----------------------------------------------------------------------------
;; Convenience
;;-----------------------------------------------------------------------------

(defn- scope-identity
  [result]
  ;; h2 only
  (first (vals (first result))))

;;-----------------------------------------------------------------------------
;; Queries
;;-----------------------------------------------------------------------------

(defn scripts
  [this]
  (doall (jdbc/query (:spec this) ["select * from script"])))

(defn save-script
  [this script]
  (let [result (jdbc/insert! (:spec this) "script"
                             {:name (:name script)
                              :crontab (:cron script)
                              :script (:text script)
                              :status "inactive"})]
    (log/infof "ID just saved is: `%s`." (scope-identity result))
    result))

;;-----------------------------------------------------------------------------
;; Bootstrap
;;-----------------------------------------------------------------------------

(defn start!
  [config]
  (let [spec (:spec config)
        subname (:subname spec)
        place (str home sep ".nashorn_app")
        dir (init-dir! place)
        spec (assoc spec :subname (format subname dir))]
    (log/infof "→ data stored in %s." place)
    (migrate! spec #'mig-001)
    {:spec spec}))

(defn stop!
  [svc]
  nil)
