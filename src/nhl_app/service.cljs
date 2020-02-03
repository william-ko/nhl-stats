(ns nhl_app.service
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn get-teams
  []
  (go (let [response (<! (http/get "https://statsapi.web.nhl.com/api/v1/teams"
                                   {:with-credentials? false}))]
        (get-in response [:body :teams]))))