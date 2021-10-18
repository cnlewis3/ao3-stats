(ns ao3-stats.sql-mod
  (:require [clojure.java.jdbc :refer :all :as jdbc]
            [clojure.tools.logging :as log])
  (:import (java.sql SQLException)))

(defn connect-db []
  (let [db-spec {:classname   "org.sqlite.JDBC"
                 :subprotocol "sqlite"
                 :subname     "D:/Inquisitor/ao3/ao3-metadata_full.sqlite"}]
    db-spec))

(defn populate-stat [db-spec stat]
  (log/info "Writing: " stat)
  (try (jdbc/insert! db-spec
                     :stats
                     stat)
       (catch SQLException e (str "Caught Exception" (.getMessage e)))))
