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
                      :app-page {}}))

;; render and state functions ;;
(defn load-teams-in-state []
  (take! (service/get-teams) (fn [teams] (doseq [team teams] (swap! state update :teams conj team)))))

(defn load-stats-in-state [link]
  (take! (service/get-team-stats link) (fn [stats] (swap! state update :stats assoc :data stats))))

(defn render-teams-list []
  (let [teams (:teams @state)]
    (for [team teams] ^{:key (:teamName team)} [:li.team
                                                [:input {:type     "button"
                                                         :value    (:name team)
                                                         :on-click #(swap! state update :app-page assoc :page :team-stats
                                                                           (load-stats-in-state (:link team)))}]])))

;; TODO: Capitalize first letter of each label
(defn keys->labels
  "Transforms map keys into readable text"
  [key]
  (let [key-split (str/split (str/replace key #"^:" "") #"(?=[A-Z])")]
    (str/join " " key-split)))

(defn render-stats [stats team-stats league-stats]
  "Renders stats on the page by key value pairs"
  [:ul.stats-list
   (map (fn [[key value]] ^{:key key} [:li (keys->labels (str key))": " value]) team-stats)])

;; components ;;
(defn teams-stats-component []
  (let [stats        (:splits (first (get-in @state [:stats :data])))
        team-stats   (:stat (first stats))
        league-stats (:stat (second stats))]
    [:div.stats-div
     [:h1 (:name (:team (second stats)))]
     (if-not (empty? (:stats @state)) (render-stats stats team-stats league-stats))
     [:div.stats-button
      [:input {:type     "button"
               :value    (str "Back")
               :on-click #(swap! state update :app-page assoc :page :teams)}]]]))

(defn teams-component []
  (swap! state update :stats assoc :data {})
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