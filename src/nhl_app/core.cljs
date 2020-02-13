(ns nhl-app.core
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.history.Html5History)
  (:require [reagent.core :as reagent :refer [atom]]
            [nhl_app.service :as service]
            [cljs.core.async :refer [take! close!]]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent]
            [clojure.string :as str]))

(enable-console-print!)

(defonce state (atom {:teams    []
                      :stats    {}
                      :roster   {}
                      :app-page {}}))

;; utility functions ;;
(defn keys->labels
  "Transforms map keys into readable text"
  [key]
  (let [key-split (str/split (str/replace key #"^:" "") #"(?=[A-Z])")]
    (str/join " " key-split)))

;; state functions ;;
(defn load-teams-in-state []
  (take! (service/get-teams) (fn [teams] (doseq [team teams] (swap! state update :teams conj team)))))

(defn load-data-in-state
  [channel key]
  (take! channel (fn [data] (swap! state update key assoc :data data))))

(defn clear-state
  []
  (swap! state update :stats assoc :data {})
  (swap! state update :roster assoc :data {}))

;; content rendering components ;;
(defn render-teams-list []
  (let [teams (:teams @state)]
    (for [team teams] ^{:key (:id team)} [:li.team
                                          [:input {:type     "button"
                                                   :value    (:name team)
                                                   :on-click #(swap! state update :app-page assoc :page :team-stats
                                                                     (load-data-in-state (service/get-team-roster (:link team)) :roster)
                                                                     (load-data-in-state (service/get-team-stats (:link team)) :stats))}]])))

(defn render-stats
  [stats team-stats league-stats]
  "Renders stats on the page by key value pairs"
  [:ul.stats-list
   (map (fn [[key value]] ^{:key key} [:li (keys->labels (str key)) ": " value]) team-stats)])

(defn list-players
  [player]
  (println player)
  [:li.player-list (str (get-in player [:person :fullName]))])

(defn render-roster-content
  [roster]
  [:ul.roster-list
   (map (fn [player] ^{:key (get-in player [:person :id])} (list-players player)) roster)])

(defn render-stats-content
  [stats team-stats league-stats roster]
  [:div.stats-div
   [:h1 (:name (:team (second stats)))]
   (render-stats stats team-stats league-stats)
   (render-roster-content (second roster))
   [:div.stats-button
    [:input {:type     "button"
             :value    (str "Back")
             :on-click #(swap! state update :app-page assoc :page :teams (clear-state))}]]])

;; components ;;
(defn stats-component []
  (let [stats        (:splits (first (get-in @state [:stats :data])))
        team-stats   (:stat (first stats))
        league-stats (:stat (second stats))
        roster       (first (get-in @state [:roster :data]))]
    (if-not (empty? (:stats @state)) (render-stats-content stats team-stats league-stats roster))))

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
  [stats-component])

(defn nhl-app []
  [current-page (app-routes)])

(reagent/render-component [nhl-app]
                          (. js/document (getElementById "app")))