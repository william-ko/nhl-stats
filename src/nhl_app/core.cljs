(ns nhl-app.core
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.history.Html5History)
  (:require [reagent.core :as reagent :refer [atom]]
            [nhl_app.service :as service]
            [cljs.core.async :refer [take! close!]]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent]))

(enable-console-print!)

(println "This text is printed from src/nhl-app/core.cljs. Go ahead and edit it and see reloading in action.")

(defonce state (atom {:teams    []
                      :stats    []
                      :app-page {}}))

;; render and state functions ;;
;; TODO: DRY this up
(defn load-teams-in-state []
  (take! (service/get-teams) (fn [teams] (doseq [team teams] (swap! state update :teams conj team)))))

(defn load-stats-in-state [link]
  (take! (service/get-team-stats link) (fn [stats] (doseq [stat stats] (swap! state update :stats conj stat)))))

(defn render-teams-list []
  (let [teams (:teams @state)]
    (for [team teams] ^{:key (:teamName team)} [:li.team
                                                [:input {:type     "button"
                                                         :value    (:name team)
                                                         :on-click #(swap! state update :app-page assoc :page :team-stats
                                                                           (load-stats-in-state (:link team)))}]])))
;; components ;;
(defn teams-stats-component []
  [:div.stats-div
   ;; TODO: render the stats on the page
   [:ul.stats-list (if-not (empty? (:stats @state)) (println (:stats @state)))
    [:input {:type     "button"
             :value    (str "Back")
             :on-click #(swap! state update :app-page assoc :page :teams)}]]])

(defn teams-component []
  [:div.teams-div
   [:ul.team-list (if (empty? (:teams @state)) (load-teams-in-state) (render-teams-list))]])

;; client-side routing ;;
(defn hook-browser-navigation! []
  (doto (Html5History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  (defroute "/" []
            (swap! state update :app-page assoc :page :teams))

  (defroute "/stats" []
            (swap! state update :app-page assoc :page :team-stats))
  (hook-browser-navigation!))

(defmulti current-page #((:app-page @state) :page))

(defmethod current-page :teams []
  [teams-component])

(defmethod current-page :team-stats []
  [teams-stats-component])

(defn nhl-app []
  [current-page (app-routes)])

(reagent/render-component [nhl-app]
                          (. js/document (getElementById "app")))