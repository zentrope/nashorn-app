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

(defn- ->editor
  []
  (println "Creating editor: if you see this a lot, things are broken.")
  (let [place (.getElementById js/document "CodeMirrorEditor")]
    (.fromTextArea js/CodeMirror place editor-config)))

(defn- code
  [cm]
  (.getValue cm))

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
  [cm ch]
  (println "ControlBar")
  [:div.ControlBar
   [:button {:disabled true} "Save"]
   (when-not (nil? cm)
     [:button {:onClick #(do-send! ch :script/test {:script (code cm)})} "Test"])
   [:button {:onClick (send! ch :script/done)} "Done"]])

(defcs NewScriptForm < PureMixin
  (rum/local {} :this/ed)
  {:did-mount (fn [state]
                (let [cm (->editor)]
                  (reset! (:this/ed state) cm)
                  state))}
  [locals state ch]
  [:section.EditorArea
   (EditorPanel @(:this/ed locals) ch)
   (when-let [result (:script/test-result state)]
     (ResultPanel result ch))
   (ControlBar @(:this/ed locals) ch)])
