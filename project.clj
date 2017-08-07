(defproject blutwurst "0.2.0-SNAPSHOT"
  :description "A command-line utility to populate database tables with test data."
  :url "https://www.github.com/michaeljmcd/blutwurst"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"] 
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.trace "0.7.9"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/core.incubator "0.1.4"]

                 ;TODO: handle drivers sanely
                 [org.xerial/sqlite-jdbc "3.19.3"]
                 [com.h2database/h2 "1.4.195"]]
  :plugins [[quickie "0.4.1"]]
  :main ^:skip-aot blutwurst.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
