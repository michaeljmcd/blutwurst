(ns blutwurst.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [clojure.tools.trace :as trace]
            [blutwurst.core :as core]
            [blutwurst.in-memory-db-utilities :refer :all]))

(use-fixtures :each db-fixture)

;(deftest integration-tests
; (testing "End to end flow."
;  (core/-main "app.jar" "-c" connection-string "-f" :csv "-o" "-")
  ; TODO: add a new test
;  ))
