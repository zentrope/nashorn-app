(ns nashorn.client.editor
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as string :refer [blank?]]
   [cljsjs.codemirror]
   [cljsjs.codemirror.mode.javascript]
   [nashorn.client.cron :as cron]
   [nashorn.client.ui :refer [do-send! send! PureMixin WorkArea Button]]
   [rum.core :as rum :refer [defc defcs]]))

(def ^:private default-code
  (->> ["// Replace this code with your own.\n//\n"
        "var FNS = Java.type('com.bobo.nashorn.Functions');\n"
        "print(FNS.format('Hello %s!', 'World'));\n"
        "'hello, world';"]
       (string/join "\n")))

(def ^:private editor-config
  (clj->js {"value"           default-code
            "mode"            "javascript"
            "viewportMargin"  js/Infinity
            "lineWrapping"    false
            "lineNumbers"     true}))

(defn- target-val
  [e]
  (.-value (.-target e)))

(defn- mk-editor
  [onChange]
  (let [place (.getElementById js/document "CodeMirrorEditor")]
    (doto (.fromTextArea js/CodeMirror place editor-config)
      (.on "change" #(onChange (.getValue %1))))))

(defn- saveable?
  [{:keys [text cron name] :as script}]
  (and (not (blank? text))
       (not (blank? cron))
       (not (blank? name))))

(defc NamePanel < PureMixin
  [name onChange]
  [:div.NamePanel
   [:input {:type "text"
            :placeholder "Script name"
            :max-length "30"
            :value name
            :on-change #(onChange (target-val %))}]])

(defc CronPanel < PureMixin
  [cron onChange]
  (let [{:keys [error? text] :as desc} (cron/describe cron)]
    [:div.CronPanel
     [:div.Widget
      [:input {:type "text"
               :placeholder "* * * * *"
               :value cron
               :max-length "100"
               :on-change #(onChange (target-val %))}]
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
   [:div.Title name]
   [:div.Result [:pre data]]])

(defc ResultPanel < PureMixin
  [result ch]
  [:section.ResultPanel
   (result-header result ch)
   [:div.ResultBlocks
    (when (:isError result)
      (result-block "Error:" (:error result)))
    (when-not (blank? (:result result))
      (result-block "Returned result" (:result result)))
    (when-not (blank? (:output result))
      (result-block "Captured stdout" (:output result)))]])

(defc ControlBar < PureMixin
  [{:keys [text name cron] :as script} ch]
  [:div.ControlBar
   (Button {:type :save
            :disabled? (not (saveable? script))
            :onClick (send! ch :script/save {:script script})
            :label "Save"})
   (Button {:type :run
            :onClick #(do-send! ch :script/test {:text text})
            :label "Test run"})
   (Button {:type :close
            :onClick (send! ch :script/done)
            :label "Done"})])

(defcs Editor < PureMixin
  (rum/local nil :this/ed)
  (rum/local "" :this/name)
  (rum/local default-code :this/text)
  (rum/local "* * * * *" :this/cron)
  {:did-mount (fn [state]
                (let [cm (mk-editor #(reset! (:this/text state) %))]
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
                :text @(:this/text locals)
                :cron @(:this/cron locals)} ch)])
