(ns app.rama.actions
  (:require [app.rama.module :refer [user-registration-depot* init-rama! ipc* shutdown-rama!]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]))



(defn register-user [provider-id email]
  (println "Registering user" provider-id email)
  (let [{user-id "users"} (foreign-append! @user-registration-depot*
                                           {:uuid (str (java.util.UUID/randomUUID))
                                            :provider-id provider-id
                                            :email email})]
    (if (some? user-id)
      user-id
      (throw (ex-info "Username already registered" {})))))


(comment
  (init-rama!)
  (register-user "123" "test@test.com")
  
  
  (shutdown-rama!)






  )