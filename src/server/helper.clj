(ns server.helper
  (:use [clojure.tools.logging :only (info)])
  (:require [ns-tracker.core :as tracker]))

(defmacro ?
  "A useful debugging tool when you can't figure out what's going on:
  wrap a form with ?, and the form will be printed alongside
  its result. The result will still be passed along."
  [val]
  `(let [x# ~val]
     (prn '~val '~'is x#)
     x#))