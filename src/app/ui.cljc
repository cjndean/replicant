(ns app.ui)

(defn render-frontpage [state]
  [:main.p-8.max-w-screen-lg
   [:h1.text-2xl.mb-4 "Toil and trouble: Todos over the network"]
   (when-let [todos (:todo-items state)]
     [:ul.mb-4
      (for [item todos]
        [:li.my-2
         [:span.pr-2
          (if (:todo/done? item)
            "✓"
            "▢")]
         (:todo/title item)])])
   [:button.btn.btn-primary
    (if (:loading-todos? state)
      {:disabled true}
      {:on {:click [[:backend/fetch-todo-items]]}})
    (when (:loading-todos? state)
      [:span.loading.loading-spinner])
    "Fetch todos"]])

(defn render-not-found [_]
  [:h1 "Not found"])

(defn render-page [state]
  (let [f (case (:location/page-id (:location state))
            :pages/frontpage render-frontpage
            render-not-found)]
    (f state)))













; --

;(defn render-frontpage [{:keys [problems]} _] 
;  (prn "Render frontpage")
;  [:div
;   [:h1 "In Search of Error"]
;   [:p "A repository of open problems waiting to be solved"]
;   [:ul
;    (for [{:keys [problem/title problem/id]} problems]
;      [:li
;       [:ui/a {:ui/location {:location/page-id :pages/problems
;                             :location/params {:problem/id id}}}
;        title]])]])
;
;(defn get-problem [{:keys [problems]} {:keys [location/params]}]
;  (prn "Get problem" params)
;  (->> problems
;       (filter (comp #{(:problem/id params)} :problem/id))
;       first))
;
;(defn render-problem [state location]
;  (prn "Render problem" location)
;  (let [problem (get-problem state location)]
;    [:main
;     [:h1 (or (:problem/title problem)
;              "Unknown problem")]
;     (if (-> location :location/hash-params :description)
;       (list
;        [:p (:problem/description problem)]
;        [:ui/a {:ui/location (update location :location/hash-params dissoc :description)}
;         "Hide description"])
;       (when (:problem/description problem)
;         [:ui/a {:ui/location (assoc-in location [:location/hash-params :description] "1")}
;          "Show description"]))
;     [:p
;      [:ui/a {:ui/location {:location/page-id :pages/frontpage}}
;       "Back to problem listing"]]]))
;
;(defn render-not-found [_ _]
;  (prn "Render not found")
;  [:h1 "Not found"])
;
;(defn render-page [state location]
;  (prn "Render page" location)
;  (let [f (case (:location/page-id location)
;            :pages/frontpage render-frontpage
;            :pages/problems render-problem
;            render-not-found)]
;    (f state location)))
;
;
;
;

















#_(comment
  
  ;; -- Base version --

  (defn render-frontpage [{:keys [videos]} _]
    (prn videos)
    [:div
     [:h1 "In Search of Error"]
     [:p "A repository of open problems waiting to be solved"]
     [:ul
      (for [{:keys [episode/title episode/id]} videos]
        [:li
         [:ui/a {:ui/location {:location/page-id :pages/episode
                               :location/params {:episode/id id}}}
          title]])]])
  
  (defn get-episode [{:keys [videos]} {:keys [location/params]}]
    (->> videos
         (filter (comp #{(:episode/id params)} :episode/id))
         first))
  
  (defn render-episode [state location]
    (let [episode (get-episode state location)]
      [:main
       [:h1 (or (:episode/title episode)
                "Unknown episode")]
       (if (-> location :location/hash-params :description)
         (list
          [:p (:episode/description episode)]
          [:ui/a {:ui/location (update location :location/hash-params dissoc :description)}
           "Hide description"])
         (when (:episode/description episode)
           [:ui/a {:ui/location (assoc-in location [:location/hash-params :description] "1")}
            "Show description"]))
       [:p
        [:ui/a {:ui/location {:location/page-id :pages/frontpage}}
         "Back to episode listing"]]]))
  
  (defn render-not-found [_ _]
    [:h1 "Not found"])
  
  (defn render-page [state location]
    (let [f (case (:location/page-id location)
              :pages/frontpage render-frontpage
              :pages/episode render-episode
              render-not-found)]
      (f state location)))
  
  )