(ns katello.tests.templates
  (:refer-clojure :exclude [fn])
  (:require (katello [validation :as v]
                     [api-tasks :as api])
            [clj-http.client :as http]
            [clojure.java.io :as io])
  (:use katello.tasks
        [test.tree.builder :only [data-driven]]
        [serializable.fn :only [fn]]
        [com.redhat.qe.verify :only [verify-that]]
        [com.redhat.qe.auto.bz :only [open-bz-bugs]]
        [katello.conf :only [config]]))

(def test-template-name (atom nil))
(def content (atom nil))
(def products (atom []))

(def create
  (fn []
    (create-template {:name (reset! test-template-name (uniqueify "template"))
                            :description "my test template"})))

(def setup-content
  (fn []
    (let [provider-name (uniqueify "template")
          cs-name (uniqueify "cs")]
      (api/with-admin
        (api/create-provider provider-name))
      (reset! products (take 3 (unique-names "templateProduct")))
        (doseq [product @products]
          (api/with-admin
            (api/create-product product {:provider-name provider-name
                                         :description "product to test templates"})))
        (api/with-admin
          (api/with-env (@config :first-env)
            (api/promote {:products @products}))))))

(def add-content
  (fn []
    (add-to-template @test-template-name {:products @products})))
