(ns clojure-game-kit.core-test
  (:require [clojure.test :refer :all]
            [clojure-game-kit.util :refer :all])
  (:import (org.lwjgl.util.vector Matrix4f Vector3f)))

(defn matrix4f-equals [mat1 mat2]
  (and
   (= (. mat1 m00) (. mat2 m00))
   (= (. mat1 m10) (. mat2 m10))
   (= (. mat1 m20) (. mat2 m20))
   (= (. mat1 m30) (. mat2 m30))
   (= (. mat1 m01) (. mat2 m01))
   (= (. mat1 m11) (. mat2 m11))
   (= (. mat1 m21) (. mat2 m21))
   (= (. mat1 m31) (. mat2 m31))
   (= (. mat1 m02) (. mat2 m02))
   (= (. mat1 m12) (. mat2 m12))
   (= (. mat1 m22) (. mat2 m22))
   (= (. mat1 m32) (. mat2 m32))
   (= (. mat1 m03) (. mat2 m03))
   (= (. mat1 m13) (. mat2 m13))
   (= (. mat1 m23) (. mat2 m23))
   (= (. mat1 m33) (. mat2 m33))))

(deftest matrix-wtf
  (testing "Translation is inconsistent!!!"
    (let [mat1 (Matrix4f/translate (Vector3f. 1 2 3)
                                   (Matrix4f/scale (Vector3f. 5 5 5)
                                                   (Matrix4f.)
                                                   nil)
                                   nil)
          mat1-wtf (.translate (Matrix4f.) (Vector3f. 5 10 15))
          mat2 (Matrix4f/scale (Vector3f. 5 5 5)
                               (Matrix4f/translate (Vector3f. 1 2 3)
                                                   (Matrix4f.)
                                                   nil)
                               nil)
          mat2-wtf (.scale (Matrix4f.) (Vector3f. 5 5 5))]
      (is (matrix4f-equals mat1 mat1-wtf))
      (is (matrix4f-equals mat2 mat2-wtf))
      )))
