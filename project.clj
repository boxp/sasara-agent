(defproject sasara-agent "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta2"]
                 [org.clojure/core.async "0.3.443"]
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [com.google.cloud/google-cloud-pubsub "0.25.0-beta"]
                 [com.google.cloud/google-cloud-storage "1.7.0"]]
  :profiles
  {:dev {:source-paths ["src" "dev"]}
   :uberjar {:main sasara-agent.system}})
