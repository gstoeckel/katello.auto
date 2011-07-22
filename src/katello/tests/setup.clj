(ns katello.tests.setup
  (:use [katello.conf :only [init config]]
        [com.redhat.qe.auto.selenium.selenium :only [connect browser]]
        [test-clj.testng :only [gen-class-testng]]
        [clojure.contrib.string :only [split]])
  (:require [katello.tasks :as tasks])
  (:import [org.testng.annotations BeforeSuite AfterSuite BeforeClass BeforeTest]))

;; macros to be used by test scripts to add preconditions for logged
;; in users

(defmacro beforeclass-ensure-login
  "Generates a BeforeClass method that makes sure the given user is
   logged in, and if not, logs him in with the given password."
  [username password]
  `(defn ~(with-meta 'be_user {BeforeClass {:groups ["setup"]}}) [_#]
     (tasks/ensure-current-user ~username ~password)))

(defmacro beforeclass-ensure-admin
  "Generates a BeforeClass method that ensures that the admin
   user (set in the config) is logged in."
  []
  `(beforeclass-ensure-login (@config :admin-user) (@config :admin-password)))

(defn ^{BeforeTest {:groups ["setup"]}}
  ensure_ui_up [_]
  (tasks/navigate :top-level))

(defn start-sel []
  (init)
  (let [sel-addr (@config :selenium-address)
        [host port] (split #":" sel-addr)] 
    (connect host (Integer/parseInt port) "" (@config :server-url))
    (browser start)
    (browser open (@config :server-url))))

(defn  stop-selenium []
  (browser stop))

