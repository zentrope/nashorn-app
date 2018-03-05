(ns nashorn.server.stub
  (:gen-class))

(defn -main
  [& args]
  (require 'nashorn.server.main)
  (apply (resolve 'nashorn.server.main/-main) args))
