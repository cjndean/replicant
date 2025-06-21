(ns app.frontpage
  (:require [app.query :as query]
            [app.command :as command]))

(def items-query
  {:query/kind :query/todo-items})

(defn render [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "In Search of Error"]
   [:form.flex.gap-4.mb-4
    [:input.input.input-bordered.w-full.max-w-xs
     {:type "text"
      :placeholder "New todo"
      :value (::todo-title state)
      :on {:input [[:store/assoc-in [::todo-title] :event/target.value]]}}]
    [:button.btn.btn-primary
     {:type "button"
      :on
      (when-let [title (not-empty (::todo-title state))]
        {:click [[:data/command
                  {:command/kind :command/create-todo
                   :command/data {:todo/created-by "alice"
                                  :todo/title title}}
                  {:on-success [[:store/assoc-in [::todo-title] ""]
                                [:data/query items-query]]}]]})}
     "Save todo"]]
   (when-let [todos (query/get-result state items-query)]
     [:ul.mb-4
      (for [item todos]
        (let [command {:command/kind :command/toggle-todo
                       :command/data item}]
          [:li.my-2
           [:button.cursor-pointer
            (if (command/issued? state command)
              {:disabled true}
              {:on {:click
                    [[:data/command command
                      {:on-success [[:data/query items-query]]}]]}})
            [:span.pr-2
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
     [:button.btn.btn-primary {:disabled true}
      [:span.loading.loading-spinner]
      "Fetching todos"]
     [:button.btn.btn-primary
      {:on {:click [[:data/query items-query]]}}
      "Fetch todos"])])

(def page
  {:page-id :pages/frontpage
   :route []
   :on-load (fn [location]
              [[:data/query {:query/kind :query/todo-items}]])
   :render #'render})