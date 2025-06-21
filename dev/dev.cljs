(ns dev
(:require [app.core :as app]))

(defonce store (atom
                {:problems
                 [{:problem/title "How to live forever"
                   :problem/id "how-to-live-forever"
                   :problem/description "David Sinclair proposes that the cause of aging is the loss of repair mechanism contained in genes. If this was solved, aging simply wouldn’t occur."
                   :problem/created-at "Feb 14"
                   :problem/created-by "Christian Dean"
                   :problem/followers ["John Doe" "Jane Doe" "John Doe" "Jane Doe" "John Doe" "Jane Doe" "John Doe" "Jane Doe" "John Doe" "Jane Doe"]}

                  {:problem/title "Second language acquisition"
                   :problem/id "second-language-acquisition"
                   :problem/description "Stephen Krashen has shown that acquisition comes by consuming a lot of comprehensible and interesting content. But it’s difficult to reliably get this at your level..."
                   :problem/created-at "March 21"
                   :problem/created-by "Christian Dean"
                   :problem/followers ["John Doe" "Jane Doe" "John Doe" "Jane Doe"]}]}))


(defonce el (js/document.getElementById "app"))

(defn ^:dev/after-load main []
  ;; Add additional dev-time tooling here
  (app/main store el))