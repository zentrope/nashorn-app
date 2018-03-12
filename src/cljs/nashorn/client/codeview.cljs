(ns nashorn.client.codeview
  (:require
   [clojure.string :refer [lower-case]]
   [cljsjs.highlight]
   [cljsjs.highlight.langs.python]
   [cljsjs.highlight.langs.javascript]
   [nashorn.client.ui :refer [DisplayBlock PureMixin]]
   [nashorn.client.icon :as icon]
   [rum.core :as rum :refer [defc]]))

(def style
  {:padding "10px"
   :maxHeight "400px"
   :text-overflow "ellipsis"
   :overflowX "hidden"
   :whiteSpace "pre"
   ;; :fontFamily "monospace"
   :fontFamily "inherit"
   :fontSize "10pt"})

(defn- highlight
  []
  (fn [state]
    (.highlightBlock js/hljs (.querySelector js/document "code"))
    state))

(def HighlightMixin
  {:did-mount  (highlight)
   :did-update (highlight)})

(defc CodeView < PureMixin HighlightMixin
  [script language]
  (DisplayBlock {:title "Script" :commands []}
    [:code#X111 {:class (lower-case language) :style style}
     script]))
