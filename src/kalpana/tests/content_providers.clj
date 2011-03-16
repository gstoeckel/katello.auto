(ns kalpana.tests.content-providers
  (:require [kalpana.tasks :as tasks])
  (:import [org.testng.annotations Test])
  (:use [test-clj.testng :only [gen-class-testng]]
        [com.redhat.qe.verify :only [verify]]))

(defn ^{Test {:groups ["content-providers"]}} create_simple [_]
  (tasks/create-content-provider (tasks/timestamp "auto-cp") "my description" "http://myrepo.url.com/blah/" "Red Hat" "myuser" "mypass"))

(defn ^{Test {:groups ["content-providers"]}} delete_simple [_]
  (let [cp-name (tasks/timestamp "auto-cp-delete")]
    (tasks/create-content-provider cp-name "my description" "http://myrepo.url.com/blah/" "Red Hat" "myuser" "mypass")
    (tasks/delete-content-provider cp-name)))

(comment  (defn mytest [x y z] (verify (= z (+ x y))))

          (data-driven mytest {org.testng.annotations.Test {:groups ["content-providers"]}}
                       [[1 2 3]
                        [5 10 15]
                        [-1 -1 -2]
                        [1 2 4]])) 

(gen-class-testng)