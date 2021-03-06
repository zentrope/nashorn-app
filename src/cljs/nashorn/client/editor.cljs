(ns nashorn.client.editor
  (:require
   [clojure.string :as string :refer [blank?]]
   [cljsjs.codemirror]
   [cljsjs.codemirror.mode.javascript]
   [cljsjs.codemirror.mode.python]
   [nashorn.lib.cron :as cron]
   [nashorn.client.run-result :refer [ResultPanel]]
   [nashorn.client.ui :refer [do-send! send!
                              Button Container ControlBar DisplayBlock Form
                              FormHelp Field IncludeIf PureMixin Select]]
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

(def ^:private default-form
  {:script default-code :crontab "* * * * *" :name "" :language "JavaScript"})

(defn- mk-editor
  [initial-value onChange]
  (let [place (.getElementById js/document "CodeMirrorEditor")]
    (doto (.fromTextArea js/CodeMirror place editor-config)
      (.setValue initial-value)
      (.on "change" #(onChange (.getValue %1))))))

(defn- saveable?
  [{:keys [script crontab name] :as form}]
  (and (not (blank? script))
       (not (blank? crontab))
       (not (blank? name))))

(defc MetadataPanel < PureMixin
  [{:keys [form onChange] :as spec}]
  (let [crondesc (cron/describe (:crontab form))]
    (DisplayBlock {:title "Metadata"}
      (Form (Field
             {:title "Script name"
              :type "text"
              :autoFocus "true"
              :placeholder "Script name"
              :max-length "30"
              :value (:name form)
              :onChange #(onChange :name %)})
            (Field
             {:title "Minute, hour, date, month, day"
              :type "text"
              :placeholder "* * * * *"
              :value (:crontab form)
              :max-length "100"
              :onChange #(onChange :crontab %)})
            (FormHelp
             {:message (:text crondesc)
              :alert? (:error? crondesc)})
            (Select
             {:title "Language"
              :value (:language form)
              :onChange #(onChange :language %)}
             {"javascript" "JavaScript"
              "python" "Python"})))))

(defc EditorPanel < PureMixin
  [{:keys [dirty? new?] :as spec}]
  [:section.EditorPanel
   [:div.Header
    [:div.Title "Editor"]
    (if dirty?
      [:div.Status.Dirty "UNSAVED"]
      [:div.Status (if new? "" "SAVED")])]
   [:div.EditorContainer
    [:textarea#CodeMirrorEditor
     {:autoFocus (if dirty? "true" "false")
      :value default-code}]]])

(defc DocPanel < PureMixin
  [doc ch]
  (DisplayBlock {:title (str (:name doc) " - " (:desc doc))
                 :commands [(Button {:type :close
                                     :onClick (send! ch :docs/unfocus)})]}
    [:table.Detailer
     [:tbody
      [:tr [:th "Signature"] [:td (:signature doc)]]
      [:tr [:th "Returns"]   [:td (:returns doc)]]
      [:tr [:th "Example"]   [:td.Code (:example doc)]]]]))

(defc Controls < PureMixin
  [{:keys [form onSave dirty?]} ch]
  (let [making? (nil? (:id form))
        editing? (not making?)]
    (ControlBar
      (IncludeIf
        :always  (Button {:type :close
                          :onClick (send! ch :script/done)
                          :label "Done"})
        editing? (Button {:type :save
                          :label "Save"
                          :disabled? (or (not (saveable? form)) (not dirty?))
                          :onClick #(do (onSave) (do-send! ch :script/update {:script form}))})
        making?  (Button {:type :save
                          :label "Create"
                          :disabled? (not (saveable? form))
                          :onClick #(do (onSave) (do-send! ch :script/save {:script form}))})
        :always  (Button {:type :run
                          :onClick (send! ch :script/test {:text (:script form) :language (:language form)})
                          :label "Run"})))))

(def ^:private WillMountMixin
  {:will-mount (fn [state]
                 (let [[script _ _] (:rum/args state)]
                   (reset! (:this/form state) (if (empty? script) default-form script))
                   state))})

(def ^:private DidMountMixin
  {:did-mount (fn [state]
                (let [[script _ _] (:rum/args state)
                      cm (mk-editor (or (:script script) default-code)
                                    (fn [v]
                                      (swap! (:this/form state) assoc :script v)
                                      (reset! (:this/dirty? state) true)))]
                  (reset! (:this/ed state) cm)
                  state))})

(def ^:private EdMixin
  (rum/local nil :this/ed))

(def ^:private FormMixin
  (rum/local nil :this/form))

(def ^:private DirtyMixin
  (rum/local false :this/dirty?))

(defn- update!
  [locals]
  (fn [field v]
    (swap! (:this/form locals) assoc field v)
    (reset! (:this/dirty? locals) true)))

(defcs Editor < PureMixin EdMixin FormMixin DirtyMixin WillMountMixin DidMountMixin
  [locals script run-result doc ch]
  (let [form   (:this/form locals)
        cm     (:this/ed locals)
        dirty? (:this/dirty? locals)]
    [:section
     (Container "EditorArea" {}
       (EditorPanel {:dirty? @dirty? :new? (nil? (:id @form))})
       (MetadataPanel {:form @form :onChange (update! locals)})
       (IncludeIf
         doc        (DocPanel doc ch)
         run-result (ResultPanel run-result ch)))
     (Controls {:form @form
                :dirty? @dirty?
                :onSave #(reset! dirty? false)} ch)]))
