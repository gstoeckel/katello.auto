(ns katello.setup
  (:refer-clojure :exclude [replace])
  (:require [test.tree.watcher :as watch]
            [selenium-server :refer :all] 
            [clojure.string :refer [split replace]]
            (katello [login :refer [login logout]]
                     [ui-common :as common]
                     [api-tasks :as api]
                     [client :as client]
                     [conf :refer :all]
                     [tasks :refer :all] 
                     [users :as user])
            [fn.trace :as trace]
            [com.redhat.qe.auto.selenium.selenium :refer :all])
  (:import [com.thoughtworks.selenium BrowserConfigurationOptions]))

(defn new-selenium
  "Returns a new selenium client. If running in a REPL or other
   single-session environment, set single-thread to true."
  [browser-string & [single-thread]]
  (let [[host port] (split (@config :selenium-address) #":")
        sel-fn (if single-thread connect new-sel)] 
    (sel-fn host (Integer/parseInt port) browser-string (@config :server-url))))

(def empty-browser-config (BrowserConfigurationOptions.))

(defn config-with-profile
  ([locale]
     (config-with-profile empty-browser-config locale))
  ([browser-config locale]
     (.setProfile browser-config locale)))

(defn start-selenium [& [{:keys [browser-config-opts]}]]  
  (->browser
   (start (or browser-config-opts empty-browser-config))
   ;;workaround for http://code.google.com/p/selenium/issues/detail?id=3498
   (setTimeout "180000")
   (setAjaxFinishedCondition jquery-ajax-finished)
   (open (@config :server-url) false)
   (setTimeout "60000"))
  (login (@config :admin-user) (@config :admin-password)
         {:org (@config :admin-org)}))

(defn switch-new-admin-user
  "Creates a new user with a unique name, assigns him admin
   permissions and logs in as that user."
  [user pw]
  (api/create-user user {:password pw
                         :email (str user "@myorg.org")})
  (user/assign {:user user
                :roles ["Administrator"]})
  (logout)
  ;;login and set the default org to save time later
  (login user pw {:default-org (@config :admin-org)
                  :org (@config :admin-org)}))

(defn stop-selenium []
   (browser stop))

(defn thread-runner
  "A test.tree thread runner function that binds some variables for
   each thread. Starts selenium client for each thread before kicking
   off tests, and stops it after all tests are done."
  [consume-fn]
  (fn []
    (let [thread-number (->> (Thread/currentThread) .getName (re-seq #"\d+") first Integer.)
          user (uniqueify (str (@config :admin-user) thread-number))]
      (binding [sel (new-selenium (nth (cycle *browsers*)
                                       thread-number))]
        (try 
          (start-selenium {:browser-config-opts (when-let [locale (@config :locale)]
                                                  (config-with-profile locale))})
          (switch-new-admin-user user *session-password*)
          (binding [*session-user* user]
            (consume-fn))
          (finally 
            (stop-selenium)))))))

(def runner-config 
  {:teardown (fn []
                 (when selenium-server/selenium-server 
                 (selenium-server/stop)))
   :thread-runner thread-runner
   :watchers {:stdout-log watch/stdout-log-watcher
              :screencapture (watch/on-fail
                              (fn [t _] 
                                (browser "screenCapture"
                                         "screenshots"
                                         (str 
                                          (:name t)
                                          (if (:parameters t)
                                            (str "-" (System/currentTimeMillis))
                                            "")
                                          ".png")
                                         false)))}})
