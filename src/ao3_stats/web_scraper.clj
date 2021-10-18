(ns ao3-stats.web-scraper
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]])
  (:import (java.io IOException)))
(use 'clojure.tools.logging)
(def limited-url "https://archiveofourown.org/works/search?utf8=%E2%9C%93&commit=Search&work_search%5Bquery%5D=&work_search%5Btitle%5D=&work_search%5Bcreators%5D=&work_search%5Bcomplete%5D=&work_search%5Bcrossover%5D=&work_search%5Bsingle_chapter%5D=0&work_search%5Bword_count%5D=&work_search%5Blanguage_id%5D=en&work_search%5Bfandom_names%5D=&work_search%5Brating_ids%5D=&work_search%5Bcharacter_names%5D=&work_search%5Brelationship_names%5D=&work_search%5Bfreeform_names%5D=&work_search%5Bhits%5D=%3E10&work_search%5Bkudos_count%5D=&work_search%5Bcomments_count%5D=&work_search%5Bbookmarks_count%5D=&work_search%5Bsort_column%5D=revised_at&work_search%5Bsort_direction%5D=desc")
(def date_posted_page "&page=")
(def week_pre "&work_search%5Brevised_at%5D=")
(def week_post "+weeks")

(defn fetch-url [full-url]
  (let [res (try
              {:value (html/html-resource (java.net.URL. full-url))}
              (catch IOException e
                (log/info "Error:" e)
                {:exception e}))]
    (if (:exception res)
      (do (log/info "Sleeping on: " full-url)
          (Thread/sleep 10000)
          (recur full-url))
      (:value res))))

(defn retry
  [retries f & args]
  (let [res (try {:value (apply f args)}
                 (catch Exception e
                   (if (zero? retries)
                     (throw e)
                     {:exception e})))]
    (if (:exception res)
      (recur (dec retries) f args)
      (:value res))))

(defn fetch-page-works [full-url]
  (log/info "Fetching Page")
  (html/select (fetch-url full-url) [:li.work]))

(defn fetch-stats [work]
  (let [stats (html/select work [:dl.stats])]
    {:kudos (first (map html/text (html/select stats [:dd.kudos :a])))
     :comments (first (map html/text (html/select stats [:dd.comments :a])))
     :bookmarks (first (map html/text (html/select stats [:dd.bookmarks :a])))
     :hits (first (map html/text (html/select stats [:dd.hits])))}))

(defn fetch-info [work]
  (log/info "Fetching info")
  (let [id (last
             (str/split
               (get-in work [:attrs :id]) #"_"))]
    (merge {:id id
            :StoryURL (str "https://archiveofourown.org/works/" id)}
           (fetch-stats work))))

(defn scrape-page [stat-chan week-url page]
  (log/info "Page: " page)
  (->> (str week-url date_posted_page page)
       (fetch-page-works)
       (map #(fetch-info %))
       (map #(>!! stat-chan %))
       count))

(def trans
  (comp (map fetch-page-works)
        (map fetch-info)))

(defn make-urls [url-chan week-url page-start page-end]
  (->> (range page-start page-end)
       (map #(str week-url date_posted_page %))
       (map #(a/>!! url-chan %))))

(defn get-pages [url]
  (html/text (first (html/select (fetch-url url) [:ol.pagination (html/nth-child 13) :a]))))

;(map #(a/>!! web-chan %))
(defn scrape-week [week page-start stat-chan]
  (log/info "Week: " week)
  (let [url (str limited-url week_pre week "-" (inc week) week_post)
        max-pages (get-pages url)]
    (->> (range page-start (inc (Integer. max-pages)))
         (map #(scrape-page stat-chan url %))
         print)))
