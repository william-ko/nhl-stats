(ns nhl-app.core
  (:require [reagent.core :as reagent :refer [atom]]
            [nhl_app.service :as service]
            [cljs.core.async :refer [take! close!]]))

(enable-console-print!)

(println "This text is printed from src/nhl-app/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce nhl-teams (atom {:teams []}))

(defn load-teams-in-state []
  (take! (service/get-teams) (fn [teams] (doseq [team teams] (swap! nhl-teams update :teams conj team)))))

(defn render-teams-list []
  (let [teams (:teams @nhl-teams)]
    (for [team teams] ^{:key (:teamName team)} [:li.team
                                                [:input {:type "button"
                                                         :value (:name team)}]])))
(defn teams-component []
  [:div.teams-div
   [:ul.team-list (if (empty? (:teams @nhl-teams)) (load-teams-in-state) (render-teams-list))]])

(defn nhl-app []
  [teams-component])

(reagent/render-component [nhl-app]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
