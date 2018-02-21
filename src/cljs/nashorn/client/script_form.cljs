(ns nashorn.client.script-form
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as string :refer [blank?]]
   [cljsjs.codemirror]
   [cljsjs.codemirror.mode.javascript]
   [nashorn.client.ui :refer [do-send! send! PureMixin WorkArea]]
   [rum.core :as rum :refer [defc defcs]]))

(def default-code
  (->> ["//"
        "var FNS = Java.type('com.bobo.nashorn.Functions');"
        ""
        "print(FNS.format('Hello %s!', 'World'));"
        "'hello, world';"]
       (string/join "\n")))

(def editor-config
  (clj->js {"value"           default-code
            "mode"            "javascript"
            "viewportMargin"  js/Infinity
            "lineWrapping"    false
            "lineNumbers"     true}))

(defn- create-editor
  []
  (let [place (.getElementById js/document "CodeMirrorEditor")]
    (.fromTextArea js/CodeMirror place editor-config)))

(defn- code
  [locals]
  (.getValue @(:this/ed locals)))

(defcs EditorPanel < PureMixin
  (rum/local [] :this/ed)
  {:did-mount (fn [state]
                (let [cm (create-editor)]
                  (reset! (:this/ed state) cm)
                  state))}
  [locals ch]
  [:section.EditorPanel
   [:div.EditorContainer
    [:textarea#CodeMirrorEditor
     {:autoFocus true
      :value default-code}]]
   [:div.Buttons
    [:button {:disabled true} "Save"]
    [:button {:onClick #(do-send! ch :script/test {:script (code locals)})} "Test"]
    [:button {:onClick (send! ch :script/done)} "Done"]]])

(defn- result-header
  [result ch]
  [:div {:class (if (:isError result) ["Header" "Error"] ["Header"])}
    [:div.Title "Test results"]
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

(defc NewScriptForm < PureMixin
  [state ch]
  (WorkArea
   [:section.EditorArea
    (EditorPanel ch)
    (when-let [result (:script/test-result state)]
      (ResultPanel result ch))]))
