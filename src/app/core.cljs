(ns app.core
  (:require [replicant.dom :as r]
            [clojure.walk :as walk]
            [clojure.reader :as reader]
            [app.ui :as ui]
            [app.router :as router]
            [app.query :as query]
            [replicant.alias :as alias]
            [app.user :as user]
            [app.command :as command]
            [app.frontpage :as frontpage]))


(defn routing-anchor [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href (router/location->url routes (:ui/location attrs))))]
          children)))

(alias/register! :ui/a routing-anchor)

(defn find-target-href [e]
  (some-> e .-target
          (.closest "a")
          (.getAttribute "href")))

(defn get-current-location [routes]
  (->> js/location.pathname
       (router/url->location routes)))

(defn interpolate-actions [event actions]
  (walk/postwalk
   (fn [x]
     (case x
       :event/target.value (.. event -target -value)
       ;; Add more cases as needed
       x))
   actions))

(declare execute-actions)

(defn query-backend [store query]
  (swap! store query/send-request (js/Date.) query)
  (-> (js/fetch "http://localhost:3000/query" #js {:method "POST" 
                                                   :body (pr-str query)})
      (.then #(.text %))
      (.then reader/read-string)
      (.then #(swap! store query/receive-response (js/Date.) query %))
      (.catch #(swap! store query/receive-response (js/Date.) query {:error (.-message %)}))))

(defn issue-command [store command & [{:keys [on-success]}]]
  (swap! store command/issue-command (js/Date.) command)
  (-> (js/fetch "http://localhost:3000/command" #js {:method "POST"
                                :body (pr-str command)})
      (.then #(.text %))
      (.then reader/read-string)
      (.then (fn [res]
               (swap! store command/receive-response
                      (js/Date.) command res)
               (when on-success
                 (execute-actions store on-success))))
      (.catch #(swap! store command/receive-response
                      (js/Date.) command {:error (.-message %)}))))


(defn execute-actions [store actions]
  (doseq [[action & args] actions]
    (case action
      :store/assoc-in (apply swap! store assoc-in args)
      :data/query (apply query-backend store args)
      :data/command (apply issue-command store args)  
      (println "Unknown action" action "with arguments" args))))

(def pages
  [user/page
   frontpage/page])

(def by-page-id
  (->> pages
       (map (juxt :page-id identity))
       (into {})))

(defn get-render-f [state]
  (or (get-in by-page-id [(-> state :location :location/page-id) :render])
      ui/render-page))

(defn get-location-load-actions [location]
  (when-let [f (get-in by-page-id [(:location/page-id location) :on-load])]
    (f location)))

(defn navigate! [store location]
  (let [current-location (:location @store)]
    (swap! store assoc :location location)
    (when (not= current-location location)
      (execute-actions store (get-location-load-actions location)))))

(defn route-click [e store routes]
  (let [href (find-target-href e)]
    (when-let [location (router/url->location routes href)]
      (.preventDefault e)
      (if (router/essentially-same? location (:location @store))
        (.replaceState js/history nil "" href)
        (.pushState js/history nil "" href))
      (navigate! store location))))

(defn main [store el]
  (let [routes (router/make-routes pages)]
    (add-watch
     store ::render
     (fn [_ _ _ state]
       (let [f (get-render-f state)]
         (r/render el (f state) {:alias-data {:routes routes}}))))

    (r/set-dispatch!
     (fn [event-data actions]
       (->> actions
            (interpolate-actions
             (:replicant/dom-event event-data))
            (execute-actions store))))

    (js/document.body.addEventListener
     "click"
     #(route-click % store routes))

    (js/window.addEventListener
     "popstate"
     (fn [_] (navigate! store (get-current-location routes))))

    ;; Trigger the initial render
    (navigate! store (get-current-location routes))
    (swap! store assoc :app/started-at (js/Date.))))