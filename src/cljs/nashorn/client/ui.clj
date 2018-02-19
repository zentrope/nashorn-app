(ns nashorn.client.ui)

;; Macros!

(defmacro WorkArea
  [& children]
  `[:section.WorkArea#WORKAREA
    ~@children])
