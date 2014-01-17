(ns server.google-books
  (:require [org.httpkit.client :as http]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [server.helper :refer [kebabize-keys]]
            [clojure.walk :refer [keywordize-keys]]))

(def api-key "AIzaSyDzJOr0Xk-fM8gCoLu7DwxTwvIv6V5dG-o")

(def test-url "https://www.googleapis.com/books/v1/volumes?q=neuromancer&download=epub&
                filter=ebooks&libraryRestrict=no-restrict&maxResults=10&orderBy=relevance&
                printType=books&key=")

(defn search-url
  [search-string]
  (let [base-url "https://www.googleapis.com/books/v1/volumes"
        rest-url "&download=epub&filter=ebooks&libraryRestrict=no-restrict&maxResults=10&orderBy=relevance&printType=books&key="]
    (str base-url "?q=" search-string rest-url api-key)))

(defn groom
  [results]
  (let [items   (:items results)
        volumes (map :volume-info items)]
    (map #(select-keys % [:title :authors :info-link]) volumes)))

(defn search
  [search-term]
  (let [{:keys [status headers body error] :as resp} @(http/get (search-url search-term))]
    (if error
      (println "Failed, exception: " error)
      (do 
        (println "HTTP GET success: " status)
        (let [json-in     (json/read-str body)
              keywordized (keywordize-keys json-in)
              kebabed     (kebabize-keys keywordized)
              groomed     (groom kebabed)]
          groomed)))))
