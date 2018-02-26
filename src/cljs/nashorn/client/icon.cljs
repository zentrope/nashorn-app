(ns nashorn.client.icon
  (:require
   [rum.core :as rum :refer [defc]]))

(defn mk-icon [class & paths]
  [:svg {:class (str "Icon " class)
         :width "1792"
         :height "1792"
         :viewBox "0 0 1792 1792"
         :xmlns "http://www.w3.org/2000/svg"}
   [:path {:d (apply str paths)}]])

(defc Bullet < rum/static []
  (mk-icon "Bullet"
           "M1664 896q0 209-103 385.5t-279.5 279.5-385.5 103-385.5-"
           "103-279.5-279.5-103-385.5 103-385.5 279.5-279.5 385.5-1"
           "03 385.5 103 279.5 279.5 103 385.5z"))

(defc Close < rum/static []
  (mk-icon "Close"
           "M1490 1322q0 40-28 68l-136 136q-28 28-68 28t-68-28l-294"
           "-294-294 294q-28 28-68 28t-68-28l-136-136q-28-28-28-68t"
           "28-68l294-294-294-294q-28-28-28-68t28-68l136-136q28-28 "
           "68-28t68 28l294 294 294-294q28-28 68-28t68 28l136 136q2"
           "8 28 28 68t-28 68l-294 294 294 294q28 28 28 68z"))

(defc New < rum/static []
  (mk-icon "New"
           "M1600 736v192q0 40-28 68t-68 28h-416v416q0 40-28 68t-68"
           " 28h-192q-40 0-68-28t-28-68v-416h-416q-40 0-68-28t-28-6"
           "8v-192q0-40 28-68t68-28h416v-416q0-40 28-68t68-28h192q4"
           "0 0 68 28t28 68v416h416q40 0 68 28t28 68z"))


(defc Run < rum/static []
  (mk-icon "Run"
           "M1576 927l-1328 738q-23 13-39.5 3t-16.5-36v-1472q0-26 1"
           "6.5-36t39.5 3l1328 738q23 13 23 31t-23 31z"))

(defc Save < rum/static []
  (mk-icon "Save"
           "M512 1536h768v-384h-768v384zm896 0h128v-896q0-14-10-38."
           "5t-20-34.5l-281-281q-10-10-34-20t-39-10v416q0 40-28 68t"
           "-68 28h-576q-40 0-68-28t-28-68v-416h-128v1280h128v-416q"
           "0-40 28-68t68-28h832q40 0 68 28t28 68v416zm-384-928v-32"
           "0q0-13-9.5-22.5t-22.5-9.5h-192q-13 0-22.5 9.5t-9.5 22.5"
           "v320q0 13 9.5 22.5t22.5 9.5h192q13 0 22.5-9.5t9.5-22.5z"
           "m640 32v928q0 40-28 68t-68 28h-1344q-40 0-68-28t-28-68v"
           "-1344q0-40 28-68t68-28h928q40 0 88 20t76 48l280 280q28 "
           "28 48 76t20 88z"))

(defc Play < rum/static []
  (mk-icon "Play"
           "M1312 896q0 37-32 55l-544 320q-15 9-32 9-16 0-32-8-32-1"
           "9-32-56v-640q0-37 32-56 33-18 64 1l544 320q32 18 32 55z"
           "m128 0q0-148-73-273t-198-198-273-73-273 73-198 198-73 2"
           "73 73 273 198 198 273 73 273-73 198-198 73-273zm224 0q0"
           " 209-103 385.5t-279.5 279.5-385.5 103-385.5-103-279.5-2"
           "79.5-103-385.5 103-385.5 279.5-279.5 385.5-103 385.5 10"
           "3 279.5 279.5 103 385.5z"))

(defc Stop < rum/static []
  (mk-icon "Stop"
           "M896 128q209 0 385.5 103t279.5 279.5 103 385.5-103 385."
           "5-279.5 279.5-385.5 103-385.5-103-279.5-279.5-103-385.5"
           " 103-385.5 279.5-279.5 385.5-103zm0 1312q148 0 273-73t1"
           "98-198 73-273-73-273-198-198-273-73-273 73-198 198-73 2"
           "73 73 273 198 198 273 73zm96-224q-14 0-23-9t-9-23v-576q"
           "0-14 9-23t23-9h192q14 0 23 9t9 23v576q0 14-9 23t-23 9h-"
           "192zm-384 0q-14 0-23-9t-9-23v-576q0-14 9-23t23-9h192q14"
           " 0 23 9t9 23v576q0 14-9 23t-23 9h-192z"))
