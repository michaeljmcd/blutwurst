(defproject blutwurst "0.7.0"
  :description "A command-line utility to populate database tables with test data."
  :url "https://www.github.com/michaeljmcd/blutwurst"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"] 
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/data.xml "0.2.0-alpha6"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.reader "1.3.2"]
                 [org.clojure/tools.trace "0.7.10"]
                 [org.clojure/core.incubator "0.1.4"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.github.mifmif/generex "1.0.2"]
                 [org.apache.commons/commons-math3 "3.6.1"]
                 [com.thedeanda/lorem "2.1"]
                 [cheshire "5.9.0"]]
  :plugins [[lein-print "0.1.0"]
            [lein-cljfmt "0.5.7"]
            [lein-ancient "0.6.15"]]
  :main ^:skip-aot blutwurst.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all} 
             :dev {:sources ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.3.1"]
                                  [org.clojure/java.classpath "0.3.0"]
                                  [com.h2database/h2 "1.4.199" :scope "test"]]
                   :resource-paths ["test-data"]}})
