(ns blutwurst.logging-fixture
  (:require [taoensso.timbre :as timbre :refer [with-level]]))

(defn logging-fixture [f]
  (with-level :trace
    (f)))
