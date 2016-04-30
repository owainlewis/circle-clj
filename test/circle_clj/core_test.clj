(ns circle-clj.core-test
  (:require [clojure.test :refer :all]
            [circle-clj.core :refer :all]))

(deftest path-builder-test
  (testing "should construct resources"
    (is (= "/project/foo/1"
           (path-builder "project" "foo" 1)))))
