(ns nashorn.client.ui)

;; Macros!

(defmacro WorkArea
  [& children]
  `[:section.WorkArea#WORKAREA
    ~@children])

(defmacro Table
  [cols & body]
  `[:table
    [:thead
     [:tr (for [c# ~cols] [:th c#])]]
    [:tbody ~@body]])

(defmacro DisplayBlock
  [{:keys [title commands]} & body]
  `[:div.DisplayBlock
    [:div.Header
     [:div.Title ~title]
     [:div.Commands ~@commands]]
    [:div.Body ~@body]])

(defmacro Form
  [& fields]
  `[:div.Form
    [:div.Fields
     ~@fields]])

(defmacro ControlBar
  [& buttons]
  `[:div.ControlBar
    ~@buttons])

(defmacro IncludeIf
  [& clauses]
  (list 'keep '(fn [x] x)
        (mapv (fn [[t v]] (list 'when t v))
              (partition 2 (rest `(list ~@clauses))))))
