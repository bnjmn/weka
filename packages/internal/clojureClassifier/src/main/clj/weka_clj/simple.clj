(ns weka-clj.simple
  (:import (weka.core Instances Instance)))

;; simple majority class classifier (basically ZeroR)

;; required function
(defn learn-classifier [insts options-string]
  (def class-mode (.meanOrMode insts (.classIndex insts)))
  
  class-mode
  )

;; required function
(defn distribution-for-instance [inst model]
  (def dist (if (.isNominal (.classAttribute inst)) 
              (make-array Double/TYPE (.numValues (.classAttribute inst)))
              (make-array Double/TYPE 1)))
  ;; set the index of the majority class to value 1 in the distribution (for nominal class)
  (if (.isNominal (.classAttribute inst))
    (aset dist (int model) 1.0)
    (aset dist 0 (double model)))
  dist
  )

;; optional function
(defn model-to-string [model header]
  (if (.isNominal (.classAttribute header))
    (str "Majority class: " (.value (.classAttribute header) (int model)))
    (str "Mean of class: " model))
  )
  
