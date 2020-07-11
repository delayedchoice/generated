(ns image-conversion.mask
  (:import
    [java.util Random]    
    [org.opencv.core Mat Point])
  (:require
    [clojure.string :as str]    
    [opencv4.core :as cv]
    [opencv4.utils :as u]
    [opencv4.colors.rgb :as rgb]
    [clojure.java.io :as io]
    ))

(def random-generator (Random.) )

(defn make-mask 
  ([img] (make-mask img 255))
  ([img threshold]
     (-> img
         (cv/clone)
         (cv/cvt-color! cv/COLOR_BGR2GRAY)
         (cv/threshold! threshold 255 cv/THRESH_BINARY_INV)
         (cv/median-blur! 7)
         )))

(defn make-rough-mask 
  ([img box] (make-rough-mask img box 3))
  ([img [ulx uly lrx lry] margin]
     (let  [
            cln (Mat/zeros (.size img) (.type img))
            ulx (- ulx margin)
            uly (- uly margin)
            lrx (+ lrx margin)
            lry (+ lry margin)
            ul (cv/new-point ulx uly)
            ur (cv/new-point lrx uly)
            lr (cv/new-point lrx lry)
            ll (cv/new-point ulx lry)
            poly (cv/new-matofpoint (into-array Point [ul ur lr ll]))
            _ (cv/fill-poly cln [poly] rgb/white )
            ]
         cln)))

;(defn- points->clj [mops]
;    (for [m mops]
;      (let [ps (.toList m)] 
;          (for [p ps] 
;            {:x (.-x p) :y (.-y p)})) ))

;(defn get-all-contours [img]
;  (let [contours (cv/new-arraylist)
;        mask (make-mask img 254)
;        _ (cv/find-contours  mask contours (cv/new-mat) cv/RETR_EXTERNAL cv/CHAIN_APPROX_SIMPLE)] 
;    (points->clj contours)
;   ))

(defn get-bounding-boxes-of-contours [mask]
  (let [contours (cv/new-arraylist)
        _ (cv/find-contours mask contours (cv/new-mat) cv/RETR_EXTERNAL cv/CHAIN_APPROX_SIMPLE)] 
   (for [c contours]
     (let [rect (cv/bounding-rect c)
           ulx (.x rect)   
           uly (.y rect)
           lrx (+ (.width rect) (.x rect))
           lry (+ (.y rect) (.height rect))]
       [ulx uly lrx lry]))))

(defn merge-countours [bounding-boxes] 
  "make ulx and uly negative so we can use max"
  (if (empty? bounding-boxes)          
      [0 0 0 0]
      (let [converted-bounding-boxes (map #(vector (- (nth % 0)) (- (nth % 1)) (nth % 2) (nth % 3)) bounding-boxes)
            bounding-box (reduce (fn [l1 l2] (map #(max %1 %2) l1 l2)) converted-bounding-boxes)
            [ulx uly lrx lry] bounding-box
            ulx (- ulx)
            uly (- uly)
            ]
      [ulx uly lrx lry])))

(defn get-bounding-box [mask]
  (let [bounding-boxes (get-bounding-boxes-of-contours mask)
        _ (println "boundingboxes: " bounding-boxes)
       ] 
   (merge-countours bounding-boxes)))

(defn get-internal-bounding-box [mask]
  (let [bounding-boxes (get-bounding-boxes-of-contours mask)
        _ (println "boundingboxes to filter: " bounding-boxes)
        filtered-bounding-boxes (filter (fn [[x y z w]] (> (* x y z w) 0)) bounding-boxes)
        _ (println "filteredboundingboxes: " filtered-bounding-boxes)
       ] 
   (merge-countours filtered-bounding-boxes)))


(defn- print-size [size]
  (last (str/split (.toString size) #" ") ))

(defn- print-point [size]
  (let [split-message (str/split (.toString size) #" ")] 
    (drop (- (count split-message) 2) split-message)))

(defn crop-to-internal-bounding-box 
  ([img] (crop-to-internal-bounding-box img false))
  ([img bounding-box]
    (let [
          [ulx uly lrx lry] (or bounding-box (get-internal-bounding-box (make-mask img 254)))
          ul (cv/new-point ulx uly) 
          lr (cv/new-point lrx lry)
          _ (println "coords: " (print-point ul) ":" (print-point lr))
          ]
      (Mat. img (cv/new-rect ulx uly (- lrx ulx) (- lry uly))))))

(defn get-sub-images [image]
  (let [img (cv/clone image) 
        mask (make-mask img 254)
        contours (get-bounding-boxes-of-contours mask)] 
    (for [contour  contours]
     (let [[ulx uly lrx lry] contour
           width (- lrx ulx)
           height (- lry uly)
           img (cv/clone (Mat. img (cv/new-rect ulx uly width height)) )]
       {:img img :mask mask :x-in-parent ulx :y-in-parent uly :width width :height height :contour-in-parent contour}))))


(defn get-normal-random 
  ([] (get-normal-random 0 1 random-generator))
  ([m s] (get-normal-random  m s random-generator))
  ([m s r] (-> r .nextGaussian (* s) (+ m)) ))

(defn ->contour [image-info]
  (let [ulx (:x-in-parent image-info)
        uly (:y-in-parent image-info)
        lrx (+ ulx (:width image-info))
        lry (+ uly (:height image-info))
        ]
    [ulx uly lrx lry]))

(defn add-border 
  ([img] (add-border img 10))
  ([img border-size]
    (let [
          ret (cv/Mat.)
          _ (cv/copy-make-border img 
                                 ret 
                                 border-size 
                                 border-size 
                                 border-size 
                                 border-size 
                                 cv/BORDER_CONSTANT 
                                 (cv/new-scalar 255 255 255) )
          ]
      ret)))

(defn rotate [img degrees]
 (let [img (add-border img)
       rotation-matrix (cv/get-rotation-matrix-2-d (cv/new-point (/ (.width img) 2) 
                                                                 (/ (.height img) 2)) 
                                                   degrees 
                                                   1)
       ret (cv/Mat.)
       _ (cv/warp-affine img  
                         ret 
                         rotation-matrix 
                         (cv/new-size (.width img) (.height img) ) 
                         0
                         cv/BORDER_CONSTANT 
                         (cv/new-scalar 255 255 255))
       ]
   (crop-to-internal-bounding-box ret)))

(defn random-rotate [image-info]
 (let [
      _ (cv/imwrite (:img image-info) "pre-rotated-image.png")
       warp-factor (* (get-normal-random) 8)
       rotated-image (rotate (:img image-info) warp-factor)
       _ (cv/imwrite rotated-image "rotated-image.png")
       bounding-box (get-bounding-box (make-mask rotated-image 254))
       ;_ (println "bbox: " bounding-box)
       mask (make-rough-mask rotated-image bounding-box )
       contour (->contour { :x-in-parent (:x-in-parent image-info) 
                               :y-in-parent (:y-in-parent image-info)
                               :width (.width mask)
                               :height (.height mask) })
 
       ]
   (-> image-info
       (assoc :img rotated-image)
       (assoc :mask  mask)
       (assoc :contour-in-parent  contour)
       )) 
  )
    
(defn random-resize [image-info] 
    (let [
         img (:img image-info)
         ; changing by max of 5%
         height-factor (* (get-normal-random) 5 0.01)
         width-factor (* (get-normal-random) 5 0.01)
         new-height (+ (:height image-info) (* (:height image-info) height-factor ))
         new-width (+ (:width image-info) (* (:width image-info) width-factor ))
         _ (println "size: " (:width image-info)  (:height image-info) (* (:width image-info) width-factor ) (* (:height image-info) height-factor )  )

         new-size (cv/new-size new-width new-height)
         ret (Mat. new-size (.type img))
         _ (cv/resize img ret new-size)
         bounding-box (get-bounding-box (make-mask ret 254))
         mask (make-rough-mask ret bounding-box )
         contour (->contour {:x-in-parent (:x-in-parent image-info) 
                                :y-in-parent (:y-in-parent image-info)
                                :width new-width
                                :height new-height })

         ]
     (-> image-info
         (assoc :img ret)
         (assoc :width new-width)
         (assoc :height new-height)
         (assoc :mask  mask)
         (assoc :contour-in-parent  contour)
         )) )

(defn build-image 
  ([sub-images] (build-image sub-images 4))
  ([sub-images border-buffer-size]
   (let [[ulx uly lrx lry] (merge-countours (map :contour-in-parent sub-images))
         ret (add-border (cv/bitwise-not! (Mat/zeros (cv/new-size (+ lrx border-buffer-size) 
                                                       (+ lry border-buffer-size)) 
                                          (.type (:img (first sub-images))))))] 
     (doseq [s  sub-images] 
       (let [temp-file (java.io.File/createTempFile "sub-image" ".png")
             _ (cv/imwrite (:img s) (.getName temp-file))]
         (cv/seamless-clone (add-border (:img s) 5) 
                           ret 
                           (add-border (:mask s) 5)
                           (cv/new-point (+ (/ (:width s) 2) (:x-in-parent s) ) 
                                         (+ (/ (:height s) 2) (:y-in-parent s) )) 
                           ret 
                           2)))
     (crop-to-internal-bounding-box ret)) ))

(defn make-alternate-version [image]
  (let [sub-images (get-sub-images image)
        altered-sub-images (map random-resize sub-images)
        altered-sub-images (map random-rotate altered-sub-images)
        ]
    (build-image altered-sub-images)))

(defn draw-contours [img]
  (let [mask (make-mask img 254)
        contours (get-bounding-boxes-of-contours mask)
        _ (doseq[ [ulx uly lrx lry] contours ]  
            (println "draw-contours: "  ulx ":" uly ":" lrx ":"  lry)
            (cv/rectangle img (cv/new-point ulx uly) (cv/new-point lrx lry) rgb/black 1)) ]
    img))

(defn load-image [loc]
  (let [
        img (cv/imread loc)
        bounding-box (get-bounding-box (make-mask img 254))
        width (.width img)
        height (.height img)
        mask (make-rough-mask img bounding-box)
        ]
    {:img img :width width :height height :mask mask :bounding-box bounding-box}))

(comment 
  (clojure.test/test-vars [#'image-conversion.mask-test/draw-two-char])
)

