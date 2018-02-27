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

(defmacro ControlBar
  [& buttons]
  `[:div.ControlBar
    ~@buttons])
