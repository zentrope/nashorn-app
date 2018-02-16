(ns nashorn.stub
  (:gen-class))

(defn -main
  [& args]
  (require 'nashorn.main)
  (apply (resolve 'nashorn.main/-main) args))
