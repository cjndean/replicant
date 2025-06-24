(ns app.frontpage
  (:require [app.query :as query]
            [app.command :as command]))

(def items-query
  {:query/kind :query/todo-items})

(def rama-user-query
  {:query/kind :query/rama-user 
   :query/data {:user-id "8796093022208"}})

(def login-link-command
  {:command/kind :command/get-login-link})

(def current-user-query
  {:query/kind :query/get-current-user})

(defn render [state]
  (prn "State" state)
  (let [current-user (query/get-result state current-user-query)
        authenticated? (query/available? state current-user-query)]
    [:main
     (if authenticated?
       [:div
        [:p "Welcome, " (:given_name current-user) "!"]
        [:button {:on {:click [[:data/command 
                                  {:command/kind :command/logout}
                                  {:on-success [[:store/assoc-in [:app.query/log current-user-query] nil]]}]]}}
         "Logout"]]
       [:p {:on {:click [[:data/command 
                          {:command/kind :command/get-login-link}
                          {:on-success [[:app/redirect]]}]]}}
        "Sign in with Google"])

     [:h1 "In Search of Error"] 
     [:form
      [:input
       {:type "text"
        :placeholder "New todo"
        :value (::todo-title state)
        :on {:input [[:store/assoc-in [::todo-title] :event/target.value]]}}]
      [:button
       {:type "button"
        :on
        (when (and authenticated? 
                   (not-empty (::todo-title state)))
          {:click [[:data/command
                    {:command/kind :command/create-todo
                     :command/data {:todo/created-by (:user-id current-user)
                                    :todo/title (::todo-title state)}}
                    {:on-success [[:store/assoc-in [::todo-title] ""]
                                  [:data/query items-query]]}]]})}
       "Save todo"]]
     (when-let [todos (query/get-result state items-query)]
       [:ul
        (for [item todos]
          (let [command {:command/kind :command/toggle-todo
                         :command/data item}]
            [:li
             [:button
              (if (command/issued? state command)
                {:disabled true}
                {:on {:click
                      [[:data/command command
                        {:on-success [[:data/query items-query]]}]]}})
              [:span
               (if (:todo/done? item)
                 "✓"
                 "▢")]]
             (:todo/title item)
             " ("
             [:ui/a.link
              {:ui/location
               {:location/page-id :pages/user
                :location/params {:user/id (:todo/created-by item)}}}
              (:todo/created-by item)]
             ")"]))])
     (if (query/loading? state items-query)
       [:button {:disabled true}
        [:span]
        "Fetching todos"]
       [:button
        {:on {:click [[:data/query items-query]]}}
        "Fetch todos"])]))

(def page
  {:page-id :pages/frontpage
   :route []
   :on-load (fn [location]
              [[:data/query {:query/kind :query/todo-items}]
               [:data/query current-user-query]])
   :render #'render})