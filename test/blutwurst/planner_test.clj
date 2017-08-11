(ns blutwurst.planner-test
  (:require [clojure.test :refer :all]
            [blutwurst.logging-fixture :refer :all]
            [taoensso.timbre :as timbre :refer [trace]]
            [blutwurst.planner :refer :all]))

(use-fixtures :each logging-fixture)

(deftest planner-tests
  (testing "Testing the process of sequencing tables."
    (let [person-table-def {:name "PERSON",
                             :schema "DBO",
                             :columns
                             [{:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                              {:name "ID", :type "INTEGER", :length 10, :nullable true}]}
     simple-schema {:tables (list person-table-def)}]

     (is (= (list person-table-def)
            (create-data-generation-plan simple-schema)))
    ))

  (testing "Planning should account for foreign key relationships when buidling out a plan."
   (let [city-table-definition {:name "CITY" :schema "ASDF" :columns '({:name "STATE"} {:name "COUNTRY"} {:name "NAME"})
                                        :dependencies [
                                          { :dependency-name "D1" :target-name "STATE"
                                            :target-schema "ASDF" :links {"STATE" "NAME"}}
                                          { :dependency-name "D2" :target-name "COUNTRY"
                                            :target-schema "ASDF" :links {"COUNTRY" "NAME"}}
                                        ]}
         country-table-definition {:name "COUNTRY" :schema "ASDF" :columns '({:name "NAME"}) :dependencies []}
         state-table-definition {:name "STATE" :schema "ASDF" :columns '({:name "NAME"})
                                        :dependencies []}
         foreign-key-schema {:tables (list country-table-definition city-table-definition state-table-definition)}
         result (create-data-generation-plan foreign-key-schema)]

     (trace "Table order: " (pr-str (seq (map #(str (:schema %) "." (:name %)) result))))

     (is (= (list country-table-definition state-table-definition city-table-definition)
            result
         )))
  ))
