(ns blutwurst.jsonschema-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [clojure.java.io :as io]
            [blutwurst.jsonschema :refer :all]))

(deftest json-schema-parsing
  (testing "JSON Schema parsing tests."
   (let [spec {:connection-string (io/resource "address.json")}
         expected {:tables [{:name "UNKNOWN" :schema "http://json-schema.org/draft-06/schema#" :dependencies []
                   :columns [{:name "post-office-box" :type "string" :nullable true}
                             {:name "extended-address" :type "string" :nullable true}
                             {:name "street-address" :type "string" :nullable true}
                             {:name "locality" :type "string" :nullable false}
                             {:name "region" :type "string" :nullable false}
                             {:name "postal-code" :type "string" :nullable true}
                             {:name "country-name" :type "string" :nullable false}]}]}
         result (parse-json-schema-from-spec spec)]
         (pprint result)
    (is (= expected result)))))
