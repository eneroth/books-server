(defproject server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ns-tracker "0.2.1"]
                 [http-kit "2.1.16"]
                 [camel-snake-kebab "0.1.2"]
                 [ring/ring-devel "1.1.8"]
                 [ring/ring-core "1.1.8"]
                 [jarohen/chord "0.2.2"]
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/data.fressian "0.2.0"]
                 [org.clojars.freeagent/clj-amazon "0.2.2-SNAPSHOT"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]
  :uberjar-name "server.jar"
  :main server.core
  :profiles {:uberjar {:main server.core, :aot :all}})
