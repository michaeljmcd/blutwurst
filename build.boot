(set-env! :resource-paths #{"src"}
          :dependencies '[
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [org.clojure/clojure "1.8.0"] 
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.trace "0.7.9"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/core.incubator "0.1.4"]
                 [cheshire "5.7.1"]

                 ;TODO: handle drivers sanely
                 [org.xerial/sqlite-jdbc "3.19.3"]
                 [com.h2database/h2 "1.4.195"]])
(require '[adzerk.boot-test :refer :all])
(set-env! :source-paths #{"test"})

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
   (aot :namespace #{'blutwurst.core})
   (uber)
   (jar :file "blutwurst.jar" :main 'blutwurst.core)
   (sift :include #{#"blutwurst.jar"})
   (target)))
