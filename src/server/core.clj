(ns server.core
  (:gen-class)
  (:require [chord.http-kit :refer [with-channel]]
            [ring.middleware.reload :refer [wrap-reload]]
            [server.channel-helpers :as h]
            [clj-amazon.core :as amazon-core]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clj-amazon.product-advertising :as amazon-pa]
            [clojure.core.async :refer [alts! split filter< map< map> <! <!! >! >!! put! close! go go-loop timeout]]
            [server.helper :as helper :refer [? start-nstracker]]
            [ring.middleware.reload :as reload]
            [org.httpkit.server :as httpkit]))
(defrecord Message [type val])

(def server-state (atom {:clients []}))

;; Amazon stuff
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


;; Message type based handlers
(defn search-handler
  [message out-channel]
  (println message)
  (println (str "Searching for '" (:val message) "'"))               
  (let [search-term (:val message)
        search-results (-> search-term search-amazon groom-results)
        ;mocked-results (-> search-term search-amazon-mock)
        ;search-results mocked-results
        ]
    (println "Sending search results to client")
    (pprint/pprint search-results)
    (put! out-channel (h/make-message :search-results search-results))
    (println "Done!")))

(defn heartbeat-handler
  [message out-channel]
  (println "Client heartbeat received")
  (go 
    (<! (timeout 5000))
    (>! out-channel (h/make-message :heartbeat "Sending server heartbeat"))))

(defn unknown-handler
  [message out-channel]
  (let [message-type (-> message :type name str string/trim)]
    (println (str "No handler defined for message type '" message-type "'"))
    (pprint/pprint message)))

;; Websocket handler
(defn handler [req]
  (let [headers (h/string-keys-to-keywords keyword (:headers req))]
    (println "Received request from" (:x-forwarded-for headers))
    (println "User agent" (:user-agent headers))
    (println "Handler starting..."))
  
  (with-channel 
    req ws-ch
    
    (println "Setting up channels...")
    (let [in-channel (map< h/message-to-record ws-ch)
          out-channel (map> h/record-to-message ws-ch)
          [search-channel other-channel]    (split #(h/has-type % :search)    in-channel)
          [heartbeat-channel other-channel] (split #(h/has-type % :heartbeat) other-channel)]
      (println "Channels set up.")
      
      ;; Message routing loop 
      (go-loop []
               (let [[message channel] (alts! [search-channel
                                               heartbeat-channel
                                               other-channel])]
                 (when message 
                   (condp = channel
                     search-channel    (search-handler    message out-channel)
                     heartbeat-channel (heartbeat-handler message out-channel)
                     other-channel     (unknown-handler   message out-channel))
                   (recur)))))))

(defn -main
  [& args]
  (let [first-arg (first args)
        port      (if first-arg (read-string first-arg) 5000)]
    (println "Starting on port" port)
    (println "Waiting for connection...")
    (start-nstracker)
    (httpkit/run-server (wrap-reload handler) {:port port})))







