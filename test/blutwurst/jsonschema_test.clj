(ns blutwurst.jsonschema-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [clojure.java.io :as io]
            [blutwurst.jsonschema :refer :all]))

(deftest json-schema-parsing
  (testing "JSON Schema parsing tests."
   (let [spec {:connection-string (io/resource "address.json")}
         result (parse-json-schema-from-spec spec)]
         (pprint result)
    (is (not (nil? result))))))
