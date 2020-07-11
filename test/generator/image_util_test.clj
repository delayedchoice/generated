(ns generator.image-util-test
 (:require [clojure.test :refer :all]
           [opencv4.core :as cv]
           [generator.image-util :refer :all]) )

#_(cv/imwrite cropped "resources/test/images/cropped-test.png")

(defn equal-dimensions? [image-one image-two]
  (and (pos? (.rows image-one))
       (pos? (.cols image-one))
       (= (.rows image-one) (.rows image-two))
       (= (.cols image-one) (.cols image-two))))

(defn same? [image-one image-two]
  (let [error (cv/norm image-one image-two cv/NORM_L2)
       similarity (/ error (* (.rows image-one) (.cols image-one)))
       ]
       (and (< similarity 1) (equal-dimensions? image-one image-two) )))
  

(deftest compare-mask-test
  (testing "mask/test exploration"
    (let [img (cv/imread "resources/test/4.png") 
          mask (gen-mask img)
          mask-for-test (cv/imread "resources/test/test_matches/mask-4.png")
          _ (cv/cvt-color! mask-for-test cv/COLOR_BGR2GRAY )
          ]
      (is (same? mask mask-for-test)) )))


(deftest compare-rough-mask-test
  (testing "rough-mask/test exploration"
    (let [img (cv/imread "resources/test/4.png") 
          mask (gen-rough-mask img)
          _ (cv/imwrite mask "resources/test/test_matches/rough-mask-4.png")
          mask-for-test (cv/imread "resources/test/test_matches/rough-mask-4.png")
          ]
      (is (same? mask mask-for-test)) )))

