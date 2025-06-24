(ns app.auth
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [app.rama.actions :as actions]))

(def oauth-config
  {:client-id ""
   :client-secret ""
   :redirect-uris {:dev "http://localhost:3000/authentication/callback"
                   :production "https://domain.com/authentication/callback"}
   :production-domains {:dev "domain.com"
                        :publisher "domain.com"}
   :cookie-domain ".domain.com"
   :scopes ["https://www.googleapis.com/auth/userinfo.email"
            "https://www.googleapis.com/auth/userinfo.profile"]})

(defn get-auth-url
  "Generates the Google OAuth2 authorization URL"
  []
  (let [client-id (:client-id oauth-config)
        redirect-uri (get-in oauth-config [:redirect-uris :dev])
        scopes (:scopes oauth-config)
        scope-str (clojure.string/join " " scopes)]
    (str "https://accounts.google.com/o/oauth2/auth"
         "?client_id=" client-id
         "&redirect_uri=" (java.net.URLEncoder/encode redirect-uri "UTF-8")
         "&scope=" (java.net.URLEncoder/encode scope-str "UTF-8")
         "&response_type=code"
         "&access_type=offline"
         "&prompt=consent")))

(defn handle-oauth-callback
  "Processes the OAuth callback and retrieves user info"
  [code]
  (try
    (let [client-id (:client-id oauth-config)
          client-secret (:client-secret oauth-config)
          redirect-uri (get-in oauth-config [:redirect-uris :dev])

          ;; Exchange code for tokens
          token-response (client/post "https://oauth2.googleapis.com/token"
                                      {:form-params {:code code
                                                     :client_id client-id
                                                     :client_secret client-secret
                                                     :redirect_uri redirect-uri
                                                     :grant_type "authorization_code"}
                                       :as :json})

          token-body (:body token-response)
          access-token (:access_token token-body)

          ;; Fetch user profile information
          user-response (client/get "https://www.googleapis.com/oauth2/v3/userinfo"
                                    {:headers {"Authorization" (str "Bearer " access-token)}
                                     :as :json})

          user-info (:body user-response)]

      ;; Return the enhanced user info
      {:success true :user user-info})

    (catch Exception e
      (println "OAuth error:" (.getMessage e))
      {:success false :error (.getMessage e)})))

(defn get-user-from-session
  "Get user info from session"
  [session]
  (:complete-user session))

(defn authenticated?
  "Check if user is authenticated"
  [session]
  (boolean (get-user-from-session session)))

(defn logout "Logout user"
  []
  {:status 302
   :headers {"Location" "/"}
   :session nil})

(defn authentication-callback-handler [req]
  (let [code (get-in req [:query-params "code"])]
    (if code
      (let [result (handle-oauth-callback code)]
        (if (:success result)
          (let [user (:user result)
                provider-id (:sub user)
                email (:email user)
                existing-user-id (actions/fetch-user-id provider-id)]

            (if existing-user-id

                  ; User already exists, redirect to home
              {:status 302
               :headers {"Location" "/"}
               :session (assoc (:session req)
                               :user (:user result)
                               :user-id existing-user-id)}


              ;; New user, register them 
              (try
                (let [new-user-id (actions/register-user provider-id email)]
                  (println "User registered in Rama with ID:" new-user-id)
                  {:status 302
                   :headers {"Location" "/"}
                   :session (assoc (:session req)
                                   :user (:user result)
                                   :user-id new-user-id)})
                (catch Exception e
                  (println "Error registering user in Rama:" (.getMessage e))
                  {:status 302
                   :headers {"Location" "/"}
                   :session (assoc (:session req)
                                   :user (:user result))}))))


          ;; Authentication failed - show error
          {:status 400
           :headers {"content-type" "text/html"}
           :body (str "<h1>Authentication Failed</h1><p>" (:error result) "</p>")}))
      ;; No code parameter
      {:status 400
       :headers {"content-type" "text/html"}
       :body "<h1>Authentication Failed</h1><p>No authorization code received</p>"})))