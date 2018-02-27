(ns nashorn.db
  (:require
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]
   [clojure.string :as string]
   [nashorn.logging :as log]))

;;-----------------------------------------------------------------------------

(defn- init-dir!
  [dir]
  (let [fname (string/replace dir "file://" "")]
    (.mkdirs (java.io.File. fname))))

;;-----------------------------------------------------------------------------
;; Migration infrastructure
;;-----------------------------------------------------------------------------

(def ^:private migration-table
  (jdbc/create-table-ddl
   "if not exists migration"
   [[:id :serial "primary key"]
    [:name :varchar "not null"]
    [:created_at :timestamp "not null" "default current_timestamp"]]))

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
    (jdbc/insert! conn :migration {:name m-name})))

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

(defn- pkey ;; h2 only
  [result]
  (first (vals (first result))))

;;-----------------------------------------------------------------------------
;; Queries
;;-----------------------------------------------------------------------------

(defn script-delete
  [this id]
  (let [sql "delete from script where id=?"]
    (jdbc/execute! (:spec this) [sql id])))

(defn scripts
  [this]
  (doall (jdbc/query (:spec this) ["select * from script"])))

(defn script
  [this id]
  (first (jdbc/query (:spec this) ["select * from script where id=?" id])))

(defn script-mark-run
  [this {:keys [id]}]
  (jdbc/execute! (:spec this) ["update script set last_run=now() where id=?" id]))

(defn script-save
  [{:keys [spec] :as this} {:keys [name cron text] :as new-script}]
  (let [values {:name name :crontab cron :script text :status "inactive"}
        result (jdbc/insert! spec "script" values)]
    (script this (pkey result))))

(defn script-status
  [this {:keys [id status]}]
  (let [sql "update script set status=?, updated=now() where id=?"]
    (jdbc/execute! (:spec this) [sql status id])))

;;-----------------------------------------------------------------------------
;; Bootstrap
;;-----------------------------------------------------------------------------

(defn start!
  [{:keys [spec app-dir] :as config}]
  (init-dir! app-dir)
  (log/infof "→ data stored in %s." (:subname spec))
  (migrate! spec #'mig-001)
  {:spec spec})

(defn stop!
  [svc]
  nil)
