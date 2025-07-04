(ns app.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as response]
            [clojure.string :as str]
            [app.rama.module :as module]
            [app.rama.actions :as actions]
            [app.auth :as auth]))

(defonce todos
  (atom
   [{:todo/id "74e67"
     :todo/title "Write project documentation"
     :todo/done? false
     :todo/created-at "2024-12-30T10:15:00Z"
     :todo/created-by "alice"}

    {:todo/id "a94e4"
     :todo/title "Refactor rendering logic"
     :todo/done? false
     :todo/created-at "2024-12-29T14:30:00Z"
     :todo/created-by "bob"}

    {:todo/id "c53b1"
     :todo/title "Implement state synchronization"
     :todo/done? true
     :todo/created-at "2024-12-28T09:00:00Z"
     :todo/created-by "carol"}

    {:todo/id "8d546"
     :todo/title "Implement user authentication"
     :todo/done? false
     :todo/created-at "2024-12-30T11:45:00Z"
     :todo/created-by "alice"}

    {:todo/id "a20d5"
     :todo/title "Optimize query responses"
     :todo/done? true
     :todo/created-at "2024-12-27T16:00:00Z"
     :todo/created-by "bob"}

    {:todo/id "2f4e1"
     :todo/title "Add unit tests for new features"
     :todo/done? false
     :todo/created-at "2024-12-29T13:20:00Z"
     :todo/created-by "carol"}

    {:todo/id "2b4fd"
     :todo/title "Update website design"
     :todo/done? false
     :todo/created-at "2024-12-30T09:50:00Z"
     :todo/created-by "alice"}

    {:todo/id "b085f"
     :todo/title "Research deployment strategies"
     :todo/done? false
     :todo/created-at "2024-12-29T10:40:00Z"
     :todo/created-by "bob"}]))

(defonce users
  [{:user/id "alice"
    :user/given-name "Alice"
    :user/family-name "Johnson"
    :user/email "alice.johnson@acme-corp.com"}

   {:user/id "bob"
    :user/given-name "Bob"
    :user/family-name "Smith"
    :user/email "bob.smith@acme-corp.com"}

   {:user/id "carol"
    :user/given-name "Carol"
    :user/family-name "Lee"
    :user/email "carol.lee@acme-corp.com"}])

(defn get-current-user [req]
  (if-let [user-session (get-in req [:session :user])]
    (let [user-id (get-in req [:session :user-id])
          rama-user (actions/fetch-user user-id)
          user (merge user-session rama-user {:user-id user-id})]
      {:success? true
       :result user})
    {:success? false
     :error "Not authenticated"}))

(defn query [req]
  (if-let [query (try
                   (read-string (slurp (:body req)))
                   (catch Exception e
                     (println "Failed to parse query body")
                     (prn e)))]
    (try
      (case (:query/kind query)
        :query/todo-items
        {:success? true
         :result @todos}

        :query/user
        (let [{:keys [user-id]} (:query/data query)]
          (if-let [user (first (filter (comp #{user-id} :user/id) users))]
            {:success? true
             :result user}
            {:error "No such user"}))

        :query/get-current-user
        (get-current-user req)

        :query/rama-user
        (let [{:keys [user-id]} (:query/data query)]
          (if-let [user (actions/fetch-user user-id)]
            {:success? true
             :result user}
            {:error "No such user"}))



        {:error "Unknown query type"
         :query query})
      (catch Exception e
        (println "Failed to handle query")
        (prn e)
        {:error "Failed while executing query"}))
    {:error "Unparsable query"}))


(defn random-id []
  (str/join (take 5 (str (random-uuid)))))

(defn create-todo [todo]
  (if (and (:todo/title todo)
           (:todo/created-by todo))
    (do
      (swap! todos conj {:todo/id (random-id)
                         :todo/title (:todo/title todo)
                         :todo/created-by (:todo/created-by todo)
                         :todo/done? false
                         :todo/created-at (str (java.time.Instant/now))})
      {:success? true})
    {:success? false}))

(defn toggle-todo [{:keys [todo/id]}]
  (swap! todos (fn [items]
                 (for [item items]
                   (cond-> item
                     (= id (:todo/id item))
                     (update :todo/done? not)))))
  {:success? true})

(defn get-login-link [req]
  (let [auth-url (auth/get-auth-url)]
    {:success? true
     :result auth-url}))


(defn logout-user [req]
  {:success? true
   :result "Logged out"
   :session nil})


(defn handle-command [req]
  (if-let [command (try
                     (read-string (slurp (:body req)))
                     (catch Exception e
                       (println "Failed to parse command body")
                       (prn e)))]
    (try
      (let [result (case (:command/kind command)
                     :command/create-todo
                     (create-todo (:command/data command))

                     :command/toggle-todo
                     (toggle-todo (:command/data command))

                     :command/get-login-link
                     (get-login-link req)

                     :command/get-current-user
                     (get-current-user req)

                     :command/logout
                     (logout-user req)

                     {:error "Unknown command type"
                      :command command})]
        ;; Handle session updates from commands like logout
        (if (contains? result :session)
          result
          (assoc result :session (:session req))))
      (catch Exception e
        (println "Failed to handle command")
        (prn e)
        {:error "Failed while handling command"}))
    {:error "Unparsable command"}))


(defn handler [{:keys [uri params] :as req}]
  (cond
    (= "/" uri)
    (response/resource-response "/index.html" {:root "public"})

    (= "/query" uri)
    {:status 200
     :headers {"content-type" "application/edn"}
     :body (pr-str (query req))}

    (= "/command" uri)
    (let [result (handle-command req)]
      {:status 200
       :headers {"content-type" "application/edn"}
       :body (pr-str (dissoc result :session))
       :session (:session result)})

    (= "/authentication/callback" uri)
    (auth/authentication-callback-handler req)

    :else
    {:status 404
     :headers {"content-type" "text/html"}
     :body "<h1>Page not found</h1>"}))

(defn start-server [port]
  (jetty/run-jetty
   (-> #'handler
       (wrap-params)
       (wrap-session)
       (wrap-resource "public")
       (wrap-cors :access-control-allow-origin [#".*"]
                  :access-control-allow-methods [:get :post :options]
                  :access-control-allow-headers ["Content-Type" "Accept"]))
   {:port port :join? false}))

(defn stop-server [server]
  (.stop server))

(defn -main [& args]
  (println "Initializing Rama...")
  (module/init-rama!)
  (println "Starting server on port 3000...")
  (start-server 3000)
  (println "Server started successfully!"))

(comment

  (module/init-rama!)
  (actions/register-user "123" "test@test.com")
  (module/shutdown-rama!)

  (def server (start-server 3000))
  (stop-server server))