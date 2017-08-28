(defproject blutwurst "0.5.0-SNAPSHOT"
  :description "A command-line utility to populate database tables with test data."
  :url "https://www.github.com/michaeljmcd/blutwurst"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"] 
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.reader "1.0.5"]
                 [org.clojure/tools.trace "0.7.9"]
                 [org.clojure/core.incubator "0.1.4"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.github.mifmif/generex "1.0.2"]
                 [com.thedeanda/lorem "2.1"]
                 [cheshire "5.7.1"]
                 [com.h2database/h2 "1.4.195" :scope "test"]]
  :plugins [[lein-print "0.1.0"]
            [lein-cljfmt "0.5.7"]
            [venantius/ultra "0.5.1"]]
  :main ^:skip-aot blutwurst.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
