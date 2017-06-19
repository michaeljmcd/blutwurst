(ns blutwurst.planner-test
  (:require [clojure.test :refer :all]
            [blutwurst.planner :refer :all]))

(deftest planner-tests
  (testing "Testing the process of sequencing tables."
    (let [spec nil
          simple-schema {:tables '({:name "PERSON",
                         :schema "DBO",
                         :columns
                         ({:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true})})}]
     (is (= '({:name "PERSON",
                         :schema "DBO",
                         :columns
                         ({:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true})})
          (create-data-generation-plan simple-schema)))
    )))
