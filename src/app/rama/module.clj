(ns app.rama.module
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.aggs :as aggs]
            [com.rpl.rama.test :as rtest])

  (:import [com.rpl.rama.helpers ModuleUniqueIdPState]))

(defmodule rama-module [setup topologies]

  (declare-depot setup *user-registration-depot (hash-by :provider-id))
  (declare-depot setup *user-edits-depot (hash-by :user-id))

  (let [u (stream-topology topologies "users")
        id-gen (ModuleUniqueIdPState. "$$id")]

      ; User registration
    (declare-pstate u $$user-registration {String ; username
                                           (fixed-keys-schema {:user-id Long
                                                               :uuid String})})

      ; User profiles
    (declare-pstate u $$users {Long ; user ID
                               (fixed-keys-schema {:username String
                                                   :name String
                                                   :auth-provider String
                                                   :provider-id String
                                                   :email String})})




      ; Auth Provider ID to User ID mapping
    (declare-pstate u $$provider-id->username {String ; provider-id 
                                               String}) ; Username




    (.declarePState id-gen u)

    ;; --- User ETL ---
    (<<sources u
               ; User registration - Updated to handle provider-id
               (source> *user-registration-depot :> {:keys [*uuid *provider-id *email]})
               (local-select> (keypath *provider-id) $$provider-id->username :> *existing-user-id)
               (<<if (nil? *existing-user-id)
                     ;; Generate unique username and id
                     (java-macro! (.genId id-gen "*user-id"))
                     (str "user-" *user-id :> *username)

                     ;; Write to $$user-registration
                     (local-transform> [(keypath *username)
                                        (multi-path [:user-id (termval *user-id)]
                                                    [:uuid (termval *uuid)])]
                                       $$user-registration)
                     ;; Write to $$provider-id->username
                     (|hash *provider-id)
                     (local-transform> [(keypath *provider-id) (termval *username)]
                                       $$provider-id->username)
                     ;; Write to $$users
                     (|hash *user-id)
                     (local-transform> [(keypath *user-id)
                                        (multi-path [:username (termval *username)]
                                                    [:auth-provider (termval "google")]
                                                    [:provider-id (termval *provider-id)]
                                                    [:email (termval *email)])]
                                       $$users)
                     (ack-return> *user-id)
                     (else>)
                     (ack-return> *existing-user-id))

               ; Profile edits
               (source> *user-edits-depot :> {:keys [*user-id *field *value]})
               (local-transform> [(keypath *user-id *field) (termval *value)] $$users)
               (ack-return> {:field *field :value *value}))))


(def ipc* (atom nil))

;; -- Users ----
(def user-registration-depot* (atom nil))
(def user-registration-data* (atom nil))
(def users-data* (atom nil))
(def user-edits-depot* (atom nil))

;; -- Provider -> User ID
(def provider-id->username (atom nil))

(defn init-rama! []
  (when (nil? @ipc*)
    (reset! ipc* (rtest/create-ipc))
    (rtest/launch-module! @ipc* rama-module {:tasks 4 :threads 2})

    ;; -- Users ----
    (reset! user-registration-depot* (foreign-depot @ipc* (get-module-name rama-module) "*user-registration-depot"))
    (reset! user-registration-data* (foreign-pstate @ipc* (get-module-name rama-module) "$$user-registration"))
    (reset! users-data* (foreign-pstate @ipc* (get-module-name rama-module) "$$users"))
    (reset! user-edits-depot* (foreign-depot @ipc* (get-module-name rama-module) "*user-edits-depot"))

    ;; -- Provider -> User ID 
    (reset! provider-id->username (foreign-pstate @ipc* (get-module-name rama-module) "$$provider-id->username"))

    (println "Rama module initialized")))

(defn shutdown-rama! []
  (when @ipc*
    (let [_ (close! @ipc*)]
      (reset! ipc* nil))))