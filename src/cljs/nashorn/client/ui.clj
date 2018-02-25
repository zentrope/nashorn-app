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
