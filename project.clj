(defproject server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ns-tracker "0.2.1"]
                 [http-kit "2.1.16"]
                 [ring/ring-devel "1.1.8"]
                 [ring/ring-core "1.1.8"]
                 [jarohen/chord "0.2.2"]
                 [org.clojure/data.fressian "0.2.0"]
                 [org.clojars.freeagent/clj-amazon "0.2.2-SNAPSHOT"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]
  :main ^{:skip-aot true} server.core)
