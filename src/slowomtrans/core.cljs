(ns slowomtrans.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(declare reconciler)

(def init-data
  {:active false

   :counter1 0
   :blob1 nil

   :counter2 0
   :blob2 nil

   :counter3 0
   :blob3 nil})

(defn make-blob [size]
  {:n 0
   :values (into [] (for [i (range size)]
                      {:pos i}))})

(defn run-counter [blob bchan wrap?]
  (go-loop [{n :n :as blob} blob]
    (async/>! bchan (if wrap? (atom blob) blob))
    (if (< n 50)
      (recur (update blob :n inc))
      (async/close! bchan))))

(defn start-counter [blob-size counter-key blob-key wrap?]
  (om/transact! reconciler `[(set-val {:state-key :active :value true})])
  (let [bchan (async/chan)]
    (go-loop []
             (let [blob (async/<! bchan)]
               (if blob
                 (let [{n :n} (if wrap? @blob blob)]
                   (om/transact! reconciler `[(set-val {:state-key ~counter-key :value ~n})
                                              (set-val {:state-key ~blob-key :value ~blob})])
                   (async/<! (async/timeout 1))
                   (recur))
                 (om/transact! reconciler `[(set-val {:state-key :active :value false})]))))
    (run-counter (make-blob blob-size) bchan wrap?)))

;; -----------------------------------------------------------------------------
;; Parsing

(defn read [{:keys [state]} key _]
  (let [st @state]
    (if-let [[_ value] (find st key)]
      {:value value}
      {:value :not-found})))

(defn mutate [{:keys [state] :as env} key {:keys [state-key value] :as params}]
  (if (= 'slowomtrans.core/set-val key)
    {:value {:keys [state-key]}
     :action #(swap! state assoc state-key value)}
    {:value nil}))

;; -----------------------------------------------------------------------------
;; Components

(defui DemoPanel
       static om/IQuery
       (query [this]
              '[:active :counter1 :counter2 :counter3])
       Object
       (render [this]
               (let [{:keys [active counter1 counter2 counter3] :as state} (om/props this)]
                 (dom/div
                   nil
                   (dom/div
                     nil
                     (dom/button #js {:disabled active
                                      :onClick (fn [e] (start-counter 100 :counter1 :blob1 false))}
                                 "Counter 1")
                     (dom/input #js {:type "text"
                                     :value counter1
                                     :style #js {:width "25px"}
                                     :disabled true})
                     (dom/span nil " <-- Small vector"))
                   (dom/div
                     nil
                     (dom/button #js {:disabled active
                                      :onClick (fn [e] (start-counter 10000 :counter2 :blob3 true))}
                                 "Counter 2")
                     (dom/input #js {:type "text"
                                     :value counter2
                                     :style #js {:width "25px"}
                                     :disabled true})
                     (dom/span nil " <-- Large vector (hidden from the parser in an atom)"))
                   (dom/div
                     nil
                     (dom/button #js {:disabled active
                                      :onClick (fn [e] (start-counter 10000 :counter3 :blob3 false))}
                                 "Counter 3")
                     (dom/input #js {:type "text"
                                     :value counter3
                                     :style #js {:width "25px"}
                                     :disabled true})
                     (dom/span nil " <-- Large vector"))
                   ))))

(def demo-panel (om/factory DemoPanel))

;; -----------------------------------------------------------------------------
;; Initialization

(def reconciler
  (om/reconciler {:state init-data
                  :parser (om/parser {:read read :mutate mutate})
                  ;; set the logger to nil, because formatting the log output
                  ;; slows things down and obscures the real problem.
                  :logger nil
                  }))

(om/add-root! reconciler
              DemoPanel (gdom/getElement "app"))
