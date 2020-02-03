(ns nhl-app.core
  (:require [reagent.core :as reagent :refer [atom]]
            [nhl_app.service :as service]
            [cljs.core.async :refer [take! close!]]))

(enable-console-print!)

(println "This text is printed from src/nhl-app/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce nhl-teams (atom {:teams []}))

(defn load-teams-in-state []
  (take! (service/get-teams) (fn [teams] (doseq [team teams] (swap! nhl-teams update :teams conj team)))))

(defn render-teams-list []
  (let [teams (:teams @nhl-teams)]
    (for [team teams] ^{:key (:teamName team)} [:li (:name team)])))

(defn nhl-app []
  [:div
   [:ul (if (empty? (:teams @nhl-teams)) (load-teams-in-state) (render-teams-list))]
   [:h3 "Choose a team"]])

(reagent/render-component [nhl-app]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
