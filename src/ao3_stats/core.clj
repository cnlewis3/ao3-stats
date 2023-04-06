(ns ao3-stats.core
  (:require [ao3-stats.web-scraper :refer [scrape-week]]
            [ao3-stats.sql-mod :refer :all]
            [clojure.core.async :as a :refer [>! <! >!! <!! go chan go-loop]]
            [clojure.tools.logging :as log]))
(def web-chan (a/chan))

(defn parse-files
  "Parses all files on `files-chan` with the given parallelism."
  [files-chan]
  (log/info "Entering parse-files")
  (let [out (a/chan)]
    (a/pipeline 10 out print files-chan) out))

(defn scrape-to-sql []
  (let [db (connect-db)]
    (a/go-loop []
      (let [nums (<! web-chan)]
        (populate-stat db nums)
        (recur)))
    (map #(scrape-week % 1 web-chan) (range 43 560))))

(defn read-db []
  (let [db (connect-db)]
    (add-ratio db (get-ids-from-fandom db "'Haikyuu!!'"))))