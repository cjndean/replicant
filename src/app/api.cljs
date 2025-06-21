(ns app.api)

(defn register-user [id email]
  (let [request-config #js {:method "POST"
                            :body (js/JSON.stringify (clj->js {:provider-id id :email email}))
                            :headers #js {"Content-Type" "application/json"}}]
    (-> (js/fetch "http://localhost:3000/register" request-config)
        (.then (fn [response]
                 (if (not (.-ok response))
                   (throw (js/Error. (str "HTTP error! status: " (.-status response))))
                   (.json response))))
        (.then (fn [data]
                 (js/console.log "Registration successful:" data)))
        (.catch (fn [error]
                  (js/console.error "Registration failed:" error))))))










