(ns nashorn.db
  (:require
   [clojure.string :as string]
   [clojure.java.jdbc :as jdbc]
   [clojure.java.io :as io]
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
  "Return the current time, (or milliseconds) as an SQL Timestamp
  type."
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
    (log/info "Running db migration:" m-name)
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

(defn load-and-invoke!
  "Given a file containing SQL statements and comments, return a
  vector of SQL commands."
  [conn schema-file]
  (let [schema (load-sql! schema-file)]
    (jdbc/db-do-commands conn schema)))

(defn migrate!
  "Takes a Hikari pool (or spec) and migrations, which are #'functions
  which take a connection and do something interesting with the
  schema. The name of the function is used to detect if the migration
  has already been applied, so just add more migrations as the
  application grows.

  Example\":

  (migration/migrate! #'db-initial-schema
                      #'db-top-stream-schema
                      #'db-alter-stats-table)

  You can 'select * from migrations' to see which ones have been
  applied."
  [pool & migrations]
  (jdbc/with-db-connection [conn pool]
    ;;
    ;; Always try to create the migration table.
    ;;
    (jdbc/db-do-commands conn migration-table)
    ;;
    ;; Pull out the applied migrations and run the ones
    ;; not yet applied.
    ;;
    (let [accomplished? (already-run? conn)]
      (doseq [m migrations]
        (when-not (accomplished? (-> m meta :name str))
          (run-and-record! conn m))))))

;; Migrations

(defn- mig-001
  [conn]
  (load-and-invoke! conn "sql/mig-001.sql"))

;; java -jar ~/.m2/repository/com/h2database/h2/1.4.196/h2-1.4.196.jar

(defn start!
  [config]
  (let [spec (:spec config)
        subname (:subname spec)
        dir (init-dir! (str home sep ".nashorn_app"))
        spec (assoc spec :subname (format subname dir))]
    (log/info "db spec: " (pr-str spec))
    (migrate! spec #'mig-001)
    {:spec spec}))

(defn stop!
  [svc]
  nil)
