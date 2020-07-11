(ns generator.image-util
  (:import [java.util Random]    
           [org.opencv.core Mat Point])
  (:require [clojure.string :as str]    
            [clojure.tools.logging :as log]
            [opencv4.core :as cv]
            [opencv4.utils :as u]
            [opencv4.colors.rgb :as rgb]
            [clojure.java.io :as io]))

(comment
 (defn load-image [loc]
   (let [
         img (cv/imread loc)
         bounding-box (get-bounding-box (make-mask img 254))
         width (.width img)
         height (.height img)
         mask (make-rough-mask img bounding-box)
         ]
     {:img img :width width :height height :mask mask :bounding-box bounding-box})))

(defn gen-mask 
  "switch every pixel with a value > threshold to white with a black background"
  ([img] (gen-mask img 254))
  ([img threshold]
     (-> img
         (cv/clone)
         (cv/cvt-color! cv/COLOR_BGR2GRAY)
         ;255 if white
         (cv/threshold! threshold 255 cv/THRESH_BINARY_INV)
         ;seems to work better without median blur
         ;(cv/median-blur! 7)
         )))

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

(defn get-bounding-box [img]
  (let [mask (gen-mask img) 
        bounding-boxes (get-bounding-boxes-of-contours mask)
        _ (log/debug "boundingboxes: " bounding-boxes)
       ] 
   (merge-countours bounding-boxes)))

(defn get-internal-bounding-box [img]
  (let [bounding-boxes (get-bounding-boxes-of-contours img)
        _ (log/debug "boundingboxes to filter: " bounding-boxes)
        filtered-bounding-boxes (filter (fn [[x y z w]] (> (* x y z w) 0)) bounding-boxes)
        _ (log/debug "filteredboundingboxes: " filtered-bounding-boxes)
       ] 
   (merge-countours filtered-bounding-boxes)))

(defn gen-rough-mask 
  ([img] (gen-rough-mask img 3 nil))
  ([img margin] (gen-rough-mask img margin nil))
  ([img margin bounding-box]
     (let  [[ulx uly lrx lry] (or bounding-box (get-internal-bounding-box img)) 
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

(defn crop-to-internal-bounding-box 
  ([img] (crop-to-internal-bounding-box img false))
  ([img bounding-box]
    (let [
          [ulx uly lrx lry] (or bounding-box (get-internal-bounding-box (gen-mask img 254)))
          ul (cv/new-point ulx uly) 
          lr (cv/new-point lrx lry)
          _ (log/debug "coords: " (log/debug ul) ":" (log/debug lr))
          ]
      (Mat. img (cv/new-rect ulx uly (- lrx ulx) (- lry uly))))))

(defn get-sub-images [img]
  (let [img (cv/clone img) 
        mask (make-mask img 254)
        contours (get-bounding-boxes-of-contours mask)] 
    (for [contour  contours]
     (let [[ulx uly lrx lry] contour
           width (- lrx ulx)
           height (- lry uly)
           img (cv/clone (Mat. img (cv/new-rect ulx uly width height)) )]
       {:img img :mask mask :x-in-parent ulx :y-in-parent uly :width width :height height :contour-in-parent contour}))))


