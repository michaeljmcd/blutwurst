(ns blutwurst.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.pprint :refer :all])
  (:gen-class))

(def cli-options 
  [["-o" "--output OUTPUT_DIR" "Output location."
    :default "."]
   ["-h" "--help"]])

(defn -main
  "Main command line interface that will pass in arguments and kick off the data generation process."
  [& args]
  (pprint (parse-opts args cli-options)))
