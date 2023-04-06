(ns ao3-stats.sql-mod
  (:require [clojure.java.jdbc :refer :all :as jdbc]
            [clojure.tools.logging :as log]
            [clojure.pprint :as p])
  (:import (java.sql SQLException)))

(defn connect-db []
  (let [db-spec {:classname   "org.sqlite.JDBC"
                 :subprotocol "sqlite"
                 :subname     "D:/coding/ao3-stats/ao3-metadata_full.sqlite"}]
    db-spec))

(defn populate-stat [db-spec stat]
  (log/info "Writing: " stat)
  (try (jdbc/insert! db-spec
                     :stats
                     stat)
       (catch SQLException e (str "Caught Exception" (.getMessage e)))))

(defn print-test [db-spec]
  (log/info "Reading: ")
  (p/print-table (jdbc/query db-spec (str "select tbl_name, type from sqlite_master where type = 'table'")))
  (jdbc/query db-spec (str "SELECT * FROM stats limit 5;")))

(defn get-ids-from-fandom [db-spec fandom]
  (->>
    (jdbc/query db-spec (str "SELECT * FROM metadata_full WHERE category = " fandom " limit 100"))
    (map #(re-find #"\d+" (last (vals %))))))


(defn add-ratio [db-spec ids]
  (->> ids
       (pmap #(first (jdbc/query db-spec (str "SELECT * FROM stats WHERE id = " %))))
       (filter #(and (contains? % :kudos) (> (:kudos %) 5)))
       (pmap #(assoc % :ratio (float (/ (:kudos %) (:hits %)))))
       (sort-by :ratio)
       last))

(defn add-ratio [db-spec ids]
  (->>
       (first (jdbc/query db-spec (str "SELECT * FROM stats WHERE id IN " ids)))
       (filter #(and (contains? % :kudos) (> (:kudos %) 5)))
       (pmap #(assoc % :ratio (float (/ (:kudos %) (:hits %)))))
       (sort-by :ratio)
       last))