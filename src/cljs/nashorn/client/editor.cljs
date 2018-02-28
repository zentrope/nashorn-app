(ns nashorn.client.editor
  (:require
   [clojure.string :as string :refer [blank?]]
   [cljsjs.codemirror]
   [cljsjs.codemirror.mode.javascript]
   [nashorn.client.cron :as cron]
   [nashorn.client.run-result :refer [ResultPanel]]
   [nashorn.client.ui :refer [do-send! send! PureMixin WorkArea Button ControlBar]]
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
  []
  [:section.EditorPanel
   [:div.EditorContainer
    [:textarea#CodeMirrorEditor
     {:autoFocus true
      :value default-code}]]])

(defc Controls < PureMixin
  [{:keys [id script name crontab] :as form} ch]
  (let [event (if (nil? id) :script/save :script/update)]
    (ControlBar
     (Button {:type :save
              :disabled? (not (saveable? form))
              :onClick (send! ch event {:script form})
              :label (if (nil? id) "Save" "Update")})
     (Button {:type :run
              :onClick (send! ch :script/test {:text script})
              :label "Run"})
     (Button {:type :close
              :onClick (send! ch :script/done)
              :label "Done"}))))

(def ^:private WillMountMixin
  {:will-mount (fn [state]
                 (let [[script _ _] (:rum/args state)]
                   (reset! (:this/form state) (if (empty? script) default-form script))
                   state))})

(def ^:private DidMountMixin
  {:did-mount (fn [state]
                (let [[script _ _] (:rum/args state)
                      cm (mk-editor (:script script)
                                    #(swap! (:this/form state) assoc :script %))]
                  (reset! (:this/ed state) cm)
                  state))})

(def ^:private EdMixin
  (rum/local nil :this/ed))

(def ^:private FormMixin
  (rum/local :nil :this/form))

(defcs Editor < PureMixin EdMixin FormMixin WillMountMixin DidMountMixin
  [locals script run-result ch]
  (let [form (:this/form locals)
        cm   (:this/ed locals)]
    [:section
     [:section.EditorArea
      (NamePanel (:name @form) #(swap! form assoc :name %))
      (CronPanel (:crontab @form) #(swap! form assoc :crontab %))
      (EditorPanel)
      (when run-result
        (ResultPanel run-result ch))]
     (Controls @form ch)]))
