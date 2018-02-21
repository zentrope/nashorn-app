(ns nashorn.client.ui
  (:require-macros
   nashorn.client.ui)
  (:require
   [cljs.core.async :refer [put!]]
   [clojure.walk :refer [postwalk]]
   [rum.core :as rum :refer [defc]]))

(defn do-send!
  [ch event & [msg]]
  (let [msg (if (nil? msg) {} msg)]
    (put! ch (merge {:event event} msg))))

(defn send!
  [ch event & [msg]]
  (fn [e]
    (do-send! ch event msg)))

(defn clean
  [data]
  (postwalk #(if (fn? %) :clean/fn %) data))

(def PureMixin
  {:should-update (fn [old new]
                    (not= (-> old :rum/args clean)
                          (-> new :rum/args clean)))})

(defc Button < PureMixin
  [{:keys [onClick label]}]
  [:div.Button {:onClick onClick} label])
