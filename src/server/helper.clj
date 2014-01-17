(ns server.helper
  (:use [clojure.tools.logging :only (info)])
  (:require [ns-tracker.core :as tracker]
            [camel-snake-kebab :refer [->kebab-case]]
            [clojure.walk :refer [walk postwalk]]))

(defmacro ?
  "A useful debugging tool when you can't figure out what's going on:
  wrap a form with ?, and the form will be printed alongside
  its result. The result will still be passed along."
  [val]
  `(let [x# ~val]
     (prn '~val '~'is x#)
     x#))

(defn kebabize-keys
  "Recursively transforms all map keys from strings to keywords."
  {:added "1.1"}
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(->kebab-case k) v] [k v]))]
    ;; only apply to maps
    (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))


;; Reload namespaces
(defn check-namespace-changes [track]
  (try
    (doseq [ns-sym (track)]
      (info "Reloading namespace:" ns-sym)
      (require ns-sym :reload))
    (catch Throwable e (.printStackTrace e)))
  (Thread/sleep 500))

(defn start-nstracker []
  (let [track (tracker/ns-tracker ["src" "checkouts"])]
    (doto
      (Thread.
       #(while true
          (check-namespace-changes track)))
      (.setDaemon true)
      (.start))))