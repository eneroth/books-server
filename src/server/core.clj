(ns server.core
  (:gen-class)
  (:require [chord.http-kit :refer [with-channel]]
            [server.channel-helpers :as h]
            [clj-amazon.core :as amazon-core]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clj-amazon.product-advertising :as amazon-pa]
            [clojure.core.async :refer [split filter< map< map> <! <!! >! >!! put! close! go go-loop]]
            [server.helper :as helper :refer [?]]
            [ring.middleware.reload :as reload]
            [org.httpkit.server :as httpkit]))
(defrecord Message [type val])

;; Amazon IDs
(def secret-key   "04CISiNrmOP6kH+0PmI6XVJ7tsBtO46wF2prNBhy")
(def associate-id "b944ad06744d953f704c6af472a274eee584813ae2b8a768b68277045da7178c")
(def access-key   "AKIAJ3FTJEZNETDGEVNA")

(defn search-amazon
  [search-term] 
  (amazon-core/with-signer 
    (access-key secret-key) 
    (amazon-pa/item-search :search-index "Books"
                           :keywords search-term
                           :associate-tag associate-id
                           :condition "New")))

(defn groom-results
  [raw-search-results]
  (map #(-> % :item-atributes :title) (:items raw-search-results)))

(defn search-amazon-mock
  [search-term]
  (let [num (+ 1 (rand-int 10))
        results []]
    (loop [num num
           results results]
      (if (zero? num)
        results
        (recur (dec num) (conj results (rand-int 100)))))))


;; Type checking
(defn has-type
  [message type]
  (= (keyword type) (:type message)))

(defn untyped?
  [message]
  (not (:type message)))


;; Message parsers
(defn message-to-record
  "Takes a raw message from websocket and produces a record."
  [message]
  (->> message :message read-string (apply ->Message)))

(defn record-to-message
  "Takes a record and produces a string suitable
  for transfer over websocket."
  [record]
  (-> record vals vec pr-str))

(defn map-hash-map [f m]
  (into {} (for [[k v] m] [(f k) v])))

;; Handler
(defn your-handler [req]
  (let [headers (map-hash-map keyword (:headers req))]
  (println "Received request from" (:x-forwarded-for headers))
  (println "User agent" (:user-agent headers))
  (println "Handler starting..."))
  (with-channel 
    req ws-ch
    (println "Setting up channels...")
    (let [websocket-channel (map< message-to-record ws-ch)
          websocket-channel (map> record-to-message websocket-channel)
          [search-channel other-channel] (split #(has-type % :search) websocket-channel)]
      (println "Channels set up.")
      
      (comment (go-loop
                 []
                 (>! websocket-channel (Message. :heartbeat "Server connection heartbeat!"))
                 (Thread/sleep 2000)
                 (recur)))
      
      (go-loop
        []
        (when-let [message (<! search-channel)]
          (println message)
          (println (str "Searching for '" (:val message) "'"))               
          (let [search-term (:val message)
                search-results (-> search-term search-amazon groom-results)
                ;mocked-results (-> search-term search-amazon-mock)
                ;search-results mocked-results
                ]
            (println "Sending search results to client")
            (pprint/pprint search-results)
            (>! websocket-channel (Message. :search-results search-results))
            (println "Done!"))
          (recur)))
      
      (go-loop
        []
        (when-let [message (<! other-channel)]               
          (println "Got a message with unknown type!")
          (println message)
          (recur)))
      )))

(defn -main
  [& args]
  (let [first-arg (first args)
        port      (if first-arg (read-string first-arg) 5000)]
    (println "Starting on port" port)
    (println "Waiting for connection...")
    (httpkit/run-server your-handler {:port port})))







