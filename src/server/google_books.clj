(ns server.google-books
  (:require [org.httpkit.client :as http]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [ring.util.codec :refer [url-encode]]
            [server.helper :refer [kebabize-keys]]
            [clojure.walk :refer [keywordize-keys]]))

;; Google Books constants

(def api-key "AIzaSyDzJOr0Xk-fM8gCoLu7DwxTwvIv6V5dG-o") ;; Read this from environment instead

(def base-url "https://www.googleapis.com/books/v1/volumes")

(def query-params 
  {:query              "q"               ; Full-text search query string. (string)
   :download-available "download"        ; Restrict to volumes by download availability. (string)
   :filter             "filter"          ; Filter search results. (string)
   :language-restrict  "langRestrict"    ; Restrict results to books with this language code. (string)
   :library-restrict   "libraryRestrict" ; Restrict search to this user's library. (string)
   :max-results        "maxResults"      ; Maximum number of results to return. (integer, 0-40)
   :order-by           "orderBy"         ; Sort search results. (string)
   :partner            "partner"         ; Restrict and brand results for partner ID. (string)
   :print-type         "printType"       ; Restrict to books or magazines. (string)
   :projection         "projection"      ; Restrict information returned to a set of selected fields. (string)
   :show-preorders     "showPreorders"   ; Set to true to show books available for preorder. Defaults to false. (boolean)
   :source             "source"          ; String to identify the originator of this request. (string)
   :start-index        "startIndex"      ; Index of the first result to return (starts at 0) (integer, 0+)
   :fields             "fields"          ; Selector specifying which fields to include in a partial response.
   :key                "key"})           ; API-key


;; Functions
(defn construct-url
  [query-map]
  (let [query-strings (map #(str ((key %) query-params) "=" (val %)) query-map)
        query-vector  (interpose "&" query-strings )
        query-string  (apply str query-vector)]
    (str base-url "?" query-string)))

(defn search-url
  [search-string]
  (let [query {:query (url-encode search-string)
               :max-results 10
               :print-type "books"
               :key api-key}]
    (construct-url query)))

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
