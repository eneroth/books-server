(ns server.channel-helpers)

;; Convert strings keys to keywords in a map
(defn string-keys-to-keywords [f m]
  (into {} (for [[k v] m] [(f k) v])))

;; Type checking
(defn has-type
  [message type]
  (= (keyword type) (:type message)))

(defn untyped?
  [message]
  (not (:type message)))


;; Message creation
(defrecord Message [type val])

(defn make-message
  [type val]
  (Message. type val))

;; Message conversion functions
(defn message-to-record
  "Takes a raw message from websocket and produces a record."
  [message]
  (->> message :message read-string (apply ->Message)))

(defn record-to-message
  "Takes a record and produces a string suitable
  for transfer over websocket."
  [record]
  (-> record vals vec pr-str))

(defn error-to-record
  "Takes a websocket error and converts to a standard message of type error"
  [error]
  ;(log "Converting error to message")
  (println error)
  (Message. :error (-> error :error)))

;; Message definitions
(def request-socket-close
  (Message. :request-socket-close
            "Requesting to close websocket channel"))