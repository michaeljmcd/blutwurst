(ns blutwurst.core-test
  (:require [clojure.test :refer :all]
            [blutwurst.core :as foo]))

(deftest main-tests
  (testing "Basic test of the main function's workflow."
    (with-redefs [foo/build-spec (fn [a] a)]
      (is (= {:output "." :connection-string "jdbc:h2:mem:" :format "csv"}
             (foo/-main "app.jar")
             ))
    )))

(deftest command-line-parsing-tests
  (testing "Building spec object out of command line options."
    ))
