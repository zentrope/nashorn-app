(ns nashorn.client.editor
  (:require
   [clojure.string :as string :refer [blank?]]
   [cljsjs.codemirror]
   [cljsjs.codemirror.mode.javascript]
   [nashorn.lib.cron :as cron]
   [nashorn.client.run-result :refer [ResultPanel]]
   [nashorn.client.ui :refer [do-send! send! Button Container ControlBar DisplayBlock IncludeIf PureMixin]]
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
  {:script default-code :crontab "* * * * *" :name ""})

(defn- target-val
  [e]
  (.-value (.-target e)))

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
  [{:keys [dirty?] :as spec}]
  [:section.EditorPanel
   [:div.EditorContainer
    [:textarea#CodeMirrorEditor
     {:autoFocus "true"
      :value default-code}]
    (if dirty?
      [:div.Status.Dirty "UNSAVED"]
      [:div.Status "SAVED"])]])

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
  [{:keys [id script name crontab] :as form} onSave ch]
  (let [event (if (nil? id) :script/save :script/update)]
    (ControlBar
     (Button {:type :close
              :onClick (send! ch :script/done)
              :label "Done"})
     (Button {:type :save
              :disabled? (not (saveable? form))
              :onClick #(do (onSave)
                            (do-send! ch event {:script form}))
              :label (if (nil? id) "Create" "Update")})
     (Button {:type :run
              :onClick (send! ch :script/test {:text script})
              :label "Run"}))))

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

(defn- update-form
  [locals key]
  (fn [v]
    (swap! (:this/form locals) assoc key v)
    (reset! (:this/dirty? locals) true)))

;; TODO: Unify this stuff into a single form rather than all
;;       these panels and vars

;; TODO: Consider wrapping the CodeMirror editor outside this
;;       namespace.

(defcs Editor < PureMixin EdMixin FormMixin DirtyMixin WillMountMixin DidMountMixin
  [locals script run-result doc ch]
  (let [form   (:this/form locals)
        cm     (:this/ed locals)
        dirty? (:this/dirty? locals)]
    [:section
     (Container "EditorArea" {}
       (NamePanel (:name @form) (update-form locals :name))
       (CronPanel (:crontab @form) (update-form locals :crontab))
       (EditorPanel {:dirty? @dirty?})
       (IncludeIf
         doc        (DocPanel doc ch)
         run-result (ResultPanel run-result ch)))
     (Controls @form #(reset! dirty? false) ch)]))
