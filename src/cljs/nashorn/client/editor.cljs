(ns nashorn.client.editor
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as string :refer [blank?]]
   [cljsjs.codemirror]
   [cljsjs.codemirror.mode.javascript]
   [nashorn.client.cron :as cron]
   [nashorn.client.ui :refer [do-send! send! PureMixin WorkArea]]
   [rum.core :as rum :refer [defc defcs]]))

(def ^:private default-code
  (->> ["//"
        "var FNS = Java.type('com.bobo.nashorn.Functions');"
        ""
        "print(FNS.format('Hello %s!', 'World'));"
        "'hello, world';"]
       (string/join "\n")))

(def ^:private editor-config
  (clj->js {"value"           default-code
            "mode"            "javascript"
            "viewportMargin"  js/Infinity
            "lineWrapping"    false
            "lineNumbers"     true}))

(defn- mk-editor
  []
  (let [place (.getElementById js/document "CodeMirrorEditor")]
    (.fromTextArea js/CodeMirror place editor-config)))

(defn- code
  [cm]
  (when-not (nil? cm)
    (.getValue cm)))

(defc NamePanel < PureMixin
  [name onChange]
  [:div.NamePanel
   [:input {:type "text"
            :placeholder "Script name"
            :max-length "30"
            :on-change #(onChange (.-value (.-target %)))}]])

(defc CronPanel < PureMixin
  [cron onChange]
  (let [{:keys [error? text] :as desc} (cron/describe cron)]
    (println desc)
    [:div.CronPanel
     [:div.Widget
      [:input {:type "text"
               :placeholder "* * * * *"
               :value cron
               :max-length "100"
               :on-change #(onChange (.-value (.-target %)))}]
      [:span.Help "Cron: minute, hour, day-of-month, month, day-of-week"]]
     [:div {:class (if error? ["Description" "Error"] "Description")}
      text]]))

(defc EditorPanel < PureMixin
  [editor-obj ch]
  [:section.EditorPanel
   [:div.EditorContainer
    [:textarea#CodeMirrorEditor
     {:autoFocus true
      :value default-code}]]])

(defn- result-header
  [result ch]
  [:div {:class (if (:isError result) ["Header" "Error"] ["Header"])}
    [:div.Title "Script run results"]
    [:div.Controls
     [:button {:onClick (send! ch :script/done-results)} "Close"]]])

(defn- result-block
  [name data]
  [:div.ResultBlock
   [:div.Title (str "\u25be " name)]
   [:div.Result [:pre data]]])

(defc ResultPanel < PureMixin
  [result ch]
  [:section.ResultPanel
   (result-header result ch)
   [:div.ResultBlocks
    (when (:isError result)
      (result-block "Error:" (:error result)))
    (when-not (blank? (:result result))
      (result-block "Script's returned result" (:result result)))
    (when-not (blank? (:output result))
      (result-block "Script's captured stdout" (:output result)))]])

(defc ControlBar < PureMixin
  [{:keys [text name cron] :as script} ch]
  [:div.ControlBar
   [:button {:disabled true} "Save"]
   [:button {:onClick #(do-send! ch :script/test {:text text})} "Test"]
   [:button {:onClick (send! ch :script/done)} "Done"]])

(defcs Editor < PureMixin
  (rum/local nil :this/ed)
  (rum/local "" :this/name)
  (rum/local "* * * * *" :this/cron)
  {:did-mount (fn [state]
                (let [cm (mk-editor)]
                  (reset! (:this/ed state) cm)
                  state))}
  [locals state ch]
  [:section.EditorArea
   (NamePanel @(:this/name locals) #(reset! (:this/name locals) %))
   (CronPanel @(:this/cron locals) #(reset! (:this/cron locals) %))
   (EditorPanel @(:this/ed locals) ch)
   (when-let [result (:script/test-result state)]
     (ResultPanel result ch))
   (ControlBar {:name @(:this/name locals)
                :text (code @(:this/ed locals))
                :cron @(:this/cron locals)}
               ch)])
