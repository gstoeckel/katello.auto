(ns katello.tests.organizations
  (:refer-clojure :exclude [fn])
  (:require (katello [api-tasks :as api]
                     [validation :as validate]))
  (:use katello.tasks
        katello.ui-tasks
        katello.validation
        [katello.conf :only [config]]
        [tools.verify :only [verify-that]]
        test.tree.script
        [bugzilla.checker :only [open-bz-bugs]]))

;; Functions

(defn create-test-org
  "Creates an organization named org-name via the API"
  [org-name]
  (api/with-admin-creds
    (api/create-organization org-name
                             {:description "organization used to test environments."})))

(defn get-all-org-names
  "Returns a list of the names of all the organizations in the system.
   Uses the API."
  []
  (doall (map :name
              (api/with-admin-creds
                (api/all-entities :organization)))))

(defn org-exists? [org-name]
  (some #{org-name} (get-all-org-names)))

(def org-does-not-exist? (complement org-exists?))

(defn verify-bad-org-name-gives-expected-error
  [name expected-error]
  (field-validation create-organization [name] (expect-error expected-error)))

(defn create-org-with-provider-and-repo [org-name provider-name product-name repo-name repo-url]
  (create-organization org-name {:description "org to delete and recreate"})
  (switch-org org-name)
  (create-provider {:name provider-name
                    :description "provider to del and recreate"})
  (add-product {:provider-name provider-name
                :name product-name})
  (add-repo {:name repo-name
             :provider-name provider-name
             :product-name product-name
             :url repo-url}))

;; Data (Generated)

(def bad-org-names
  (let [make-data #(list %2 %1)]
    (concat 
     (validate/variations :name-must-not-contain-characters make-data validate/invalid-character)
     (validate/variations :name-no-leading-trailing-whitespace make-data validate/trailing-whitespace))))

;; Tests

(defgroup org-tests

  (deftest "Create an organization"
    (with-unique [org-name "auto-org"]
      (create-organization     org-name)
      (verify-that         (org-exists? org-name)))

  
    (deftest "Two organizations with the same name is disallowed"
      :blockers (open-bz-bugs "726724")
      
      (with-unique [org-name "test-dup"]
        (verify-2nd-try-fails-with :name-taken-error create-organization org-name {:description "org-description"})))

  
    (deftest "Organization name is required when creating organization"
      :blockers (open-bz-bugs "726724")
      
      (name-field-required create-organization ["" {:description "org description"}]))

    
    (deftest "Verify proper error message when invalid org name is used"
      :data-driven true
      :blockers (open-bz-bugs "726724")
      
      verify-bad-org-name-gives-expected-error
      bad-org-names)

  
    (deftest "Edit an organization"
      (with-unique [org-name "auto-edit"]
        (create-test-org     org-name)
        (edit-organization   org-name     :description     "edited description")))

  
    (deftest "Search for an organization"
      :description "Search for organizations based on criteria." 
      :blockers    (open-bz-bugs "750120")
      
      (with-unique [org-name "myfoobarorg"]
        (create-test-org                               org-name)
        (verify-all-search-results-contain-criteria    :organizations        {:criteria "myfoobar"})))


    (deftest "Delete an organization"
      :blockers (open-bz-bugs "716972")
    
      (with-unique [org-name "auto-del"]
        (create-test-org              org-name)
        (delete-organization          org-name)
        (verify-that                  (org-does-not-exist? org-name)))

    
      (deftest "Create an org with content, delete it and recreate it"
        :blockers api/katello-only
        
        (with-unique [org-name       "delorg"
                      provider-name  "delprov"
                      product-name   "delprod"
                      repo-name      "delrepo"
                      repo-url       "http://blah.com/blah"]
          (try
            (create-org-with-provider-and-repo   org-name provider-name product-name repo-name repo-url)
            (switch-org                          (@config :admin-org))
            (delete-organization                 org-name)
            ;;wait for delayed job to delete org
            (Thread/sleep                        30000)
            (create-org-with-provider-and-repo   org-name provider-name product-name repo-name repo-url)
            (finally
             (switch-org                         (@config :admin-org)))))))))
