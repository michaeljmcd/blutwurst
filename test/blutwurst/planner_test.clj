(ns blutwurst.planner-test
  (:require [clojure.test :refer :all]
            [blutwurst.planner :refer :all]))

(deftest planner-tests
  (testing "Testing the process of sequencing tables."
    (let [simple-schema {:tables '({:name "PERSON",
                         :schema "DBO",
                         :columns
                         [{:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true}]})}]
     (is (= '({:name "PERSON",
                         :schema "DBO",
                         :columns
                         [{:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true}]})
          (create-data-generation-plan simple-schema)))
    ))

  (testing "Planning should account for foreign key relationships when buidling out a plan."
   (let [city-table-definition {:name "CITY" :schema "ASDF" :columns '({:name "STATE"})
                                        :dependencies [
                                          { :dependency-name "D1" :target-name "STATE"
                                            :target-schema "ASDF" :links {"STATE" "NAME"}}
                                        ]}
         state-table-definition {:name "STATE" :schema "ASDF" :columns '({:name "NAME"})
                                        :dependencies []}
         foreign-key-schema {:tables (list city-table-definition state-table-definition)}]
     (is (= (list state-table-definition city-table-definition)
            (create-data-generation-plan foreign-key-schema)
         )))
                                       
  ))
