(defproject blutwurst "0.1.0-SNAPSHOT"
  :description "A command-line utility to populate database tables with test data."
  :url "https://www.github.com/michaeljmcd/blutwurst"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"] 
                 [com.h2database/h2 "1.4.195"]]
  :main ^:skip-aot blutwurst.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
