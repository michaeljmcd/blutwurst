(ns blutwurst.value-generators
  (:import (java.util Random Date)
           (java.lang Math Integer)
           (com.thedeanda.lorem LoremIpsum)
           (com.mifmif.common.regex Generex))
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [taoensso.timbre :as timbre :refer [trace error]]))

(defn- crop-to-column-size [column input-string]
  (if (or (nil? input-string) (empty? input-string))
    input-string
    (subs input-string 0 (min (count input-string) (or (:length column) 20)))))

(defn- create-regex-generator [generator-name regex]
  {:name generator-name
   :determiner #(do % nil)
   :generator #(let [g (Generex. regex)]
                 (crop-to-column-size % (.random g)))})

(defn random-integer-under-value [max-value]
  (let [random (Random.)]
    (mod (.nextLong random) max-value)))

(defn- random-decimal []
  (let [random (Random.)]
    (.nextLong random)))

(defn random-string [max-length]
  (let [g (Generex. "([A-Za-z0-9'\" \\n\\t!@#$%&*()])+")]
    (.random g (max 0 (- max-length (* 0.5 max-length))) max-length)))

(defn- random-datetime []
  (let [r (Random.)]
    (Date. (mod (.nextLong r) 4102466400000)) ; Makes sure that our date stays under 2100-01-01.
))

(defn- column-is-string? [column]
  (= :string (:type column)))

(defn- make-resource-generator-fn [resource garbage-rows value-index]
  (let [values (drop garbage-rows (-> resource io/resource io/reader csv/read-csv))
        value-count (count values)]
    (trace values)
    (fn [c]
      (crop-to-column-size c (string/trimr (nth (nth values (random-integer-under-value value-count)) value-index))))))

(defn- list-contains-type [type-list column]
  (some #{(string/upper-case (:type column))} type-list))

(defn- calculate-maximum-word-count [column]
  (let [base-count (or (:length column) 255)]
    (min base-count 2000)))

(def value-generation-strategies
  ^{:private true}
  [{:name "State Abbreviation Selector (U.S. and Canada)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "STATE"))
    :generator (make-resource-generator-fn "state_table.csv" 1 2)}
   {:name "State Name Selector (U.S. and Canada)"
    :determiner #(do % nil)
    :generator (make-resource-generator-fn "state_table.csv" 1 1)}
   {:name "Full Name Selector (U.S.)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "FULL") (string/includes? (:name %) "NAME"))
    :generator (let [last-name-fn (make-resource-generator-fn "Names_2010Census_Top1000.csv" 3 0)
                     first-name-fn (make-resource-generator-fn "census-derived-all-first.csv" 0 0)]
                 #(crop-to-column-size % (str (last-name-fn %) " " (first-name-fn %))))}
   {:name "First Name Selector (U.S.)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "FIRST"))
    :generator (make-resource-generator-fn "census-derived-all-first.csv" 0 0)}
   {:name "Last Name Selector (U.S.)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "LAST"))
    :generator (make-resource-generator-fn "Names_2010Census_Top1000.csv" 3 0)}
   {:name "City Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "CITY"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getCity l))))}
   {:name "Email Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "EMAIL"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getEmail l))))}
   {:name "Phone Number Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "PHONE"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getPhone l))))}
   {:name "Postal Code Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "ZIP"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getZipCode l))))}
   {:name "URL Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "URL"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getUrl l))))}
   {:name "Text Generator"
    :determiner column-is-string?
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getWords l 0 (calculate-maximum-word-count c)))))}
   {:name "String Generator"
    :determiner column-is-string?
    :generator #(random-string (or (min (:length %) 2000) 255))}
   {:name "Integer Generator"
    :determiner #(= (:type %) :integer)
    :generator #(random-integer-under-value (or (-> % :constraints :maximum-value) Integer/MAX_VALUE))}
   {:name "Decimal Generator"
    :determiner #(= (:type %) :decimal)
    :generator (fn [c] (random-decimal)) ; TODO account for column's max value
}
   {:name "Date / Timestamp Generator"
    :determiner #(= (:type %) :datetime) 
    :generator (fn [c] (random-datetime))}
   {:name "Null Value Generator"
    :determiner #(do % nil)
    :generator #(do % nil)}])

(defn- accrete-regex-generators [spec generators]
  (concat (map #(create-regex-generator (:name %) (:regex %)) (:regex-generators spec))
          generators))

(def create-generators (memoize (fn [spec]
                                  (->> value-generation-strategies
                                       (accrete-regex-generators spec)))))

(defn retrieve-registered-generators [spec]
  (map :name (create-generators spec)))
