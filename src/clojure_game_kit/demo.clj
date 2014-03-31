(ns clojure-game-kit.demo
  (:import (org.lwjgl.util.vector Matrix4f Vector3f)
           (org.lwjgl.opengl GL11 GL13 GL15)
           (org.lwjgl.input Keyboard Mouse))
  (:require [clojure-game-kit.vbo :refer :all]
            [clojure-game-kit.mesh :refer :all]
            [clojure-game-kit.shader :refer :all]
            [clojure-game-kit.opengl :refer :all]
            [clojure-game-kit.util :refer :all]
            [clojure-game-kit.texture :refer :all]
            [clojure-game-kit.input :refer :all])
  (:gen-class))

(defn make-state [pos grab-mouse]
  {:pos pos :h-angle 0 :v-angle 0 :mouse-grab grab-mouse :debug false :pressed-keys #{} :pressed-buttons #{}})

(def-edge-trigger key-edge-trigger-function
  [mouse-grab (:mouse-grab state)
   debug (:debug state)]
  (do
    (debug-println "Unbound (edge-trigger) key" (Keyboard/getKeyName key) "!")
    state)
  Keyboard/KEY_ESCAPE state (assoc state :mouse-grab (not mouse-grab))
  Keyboard/KEY_F1 state (assoc state :debug (not debug)))

(def-level-trigger key-level-trigger-function
  [pos (:pos state)
   h-angle (:h-angle state)
   v-angle (:v-angle state)
   rt-angle (- h-angle (/ Math/PI 2))
   lf-angle (+ h-angle (/ Math/PI 2))
   up-angle (- v-angle (/ Math/PI 2))
   dn-angle (+ v-angle (/ Math/PI 2))]
  (do
    (debug-println "Unbound (level-trigger) key" (Keyboard/getKeyName key) "!")
    state)
  Keyboard/KEY_Q (throw (Exception. "Exit requested"))
  Keyboard/KEY_W (assoc state
                   :pos (.translate pos (* -0.1 (Math/sin h-angle))
                                    0
                                    (* -0.1 (Math/cos h-angle))))
  Keyboard/KEY_S (assoc state
                   :pos (.translate pos (* 0.1 (Math/sin h-angle))
                                    0
                                    (* 0.1 (Math/cos h-angle))))
  Keyboard/KEY_A (assoc state
                   :pos (.translate pos (* 0.1 (Math/sin rt-angle))
                                    0
                                    (* 0.1 (Math/cos rt-angle))))
  Keyboard/KEY_D (assoc state
                   :pos (.translate pos (* 0.1 (Math/sin lf-angle))
                                    0
                                    (* 0.1 (Math/cos lf-angle))))
  Keyboard/KEY_LEFT (assoc state
                      :h-angle (+ h-angle 0.01))
  Keyboard/KEY_RIGHT (assoc state
                       :h-angle (- h-angle 0.01))
  Keyboard/KEY_UP (assoc state
                    :v-angle (+ v-angle 0.01))
  Keyboard/KEY_DOWN (assoc state
                      :v-angle (- v-angle 0.01))
  0 state)

(def-edge-trigger mouse-edge-trigger-function
  []
  (do
    (debug-println "Unbound (edge-trigger) button" (Mouse/getButtonName key) "!")
    state)
  0 (do (debug-println "Button press: 0") state) (do (debug-println "Button release: 0") state)
  1 (do (debug-println "Button press: 1") state) (do (debug-println "Button release: 1") state)
  2 (do (debug-println "Button press: 2") state) (do (debug-println "Button release: 2") state))

(def-level-trigger mouse-level-trigger-function
  []
  (do
    (debug-println "Unbound (level-trigger) button" (Mouse/getButtonName key) "!")
    state)
  0 (do (debug-println "Button is pressed: 0") state)
  1 (do (debug-println "Button is pressed: 1") state)
  2 (do (debug-println "Button is pressed: 2") state))

(defn motion-function [state dx dy dwheel]
  ;(printf "dx:%s dy:%s dw:%s\n" dx dy dwheel)
  (let [h-angle (:h-angle state)
        v-angle (:v-angle state)]
    (if (:mouse-grab state)
      (assoc
          (assoc state
            :h-angle (- h-angle (* 0.01 dx)))
        :v-angle (+ v-angle (* 0.01 dy)))
      state)))

(def-edge-trigger-input-loop mouse-edge-trigger-loop mouse-edge-trigger-function)
(def-level-trigger-input-loop mouse-level-trigger-loop mouse-level-trigger-function)
(def-mouse-function mouse-function mouse-edge-trigger-loop mouse-level-trigger-loop motion-function)

(def-edge-trigger-input-loop edge-trigger-input-loop key-edge-trigger-function)
(def-level-trigger-input-loop level-trigger-input-loop key-level-trigger-function)
(def-process-input process-input mouse-function edge-trigger-input-loop level-trigger-input-loop)

(def attrs (opengl-attrs :debug-mode true))

(defn run-test []
  (with-opengl 800 600 "TEST" attrs
    (let [start-time (get-time)
          main-shader (load-shader "resources/main.shader")
          tex (load-texture-2d "resources/stone.png"
                               GL13/GL_TEXTURE0
                               GL11/GL_LINEAR_MIPMAP_LINEAR
                               GL11/GL_NEAREST
                               GL11/GL_REPEAT
                               GL11/GL_REPEAT)
          pos (Vector3f. 0.0 0.0 10.0)
          base-transform (make-matrix4f
                          1.0 0.0 0.0 0.0
                          0.0 1.0 0.0 0.0
                          0.0 0.0 1.0 0.0
                          0.0 0.0 0.0 1.0)
          projection-matrix (make-projection-matrix 60 800 600 0.1 100.0)
          ;view-matrix (translate-matrix (Matrix4f.) (Vector3f. 0.0 0.0 -10.0))
          projection-matrix-location (get (:uniforms main-shader) "proj")
          view-matrix-location (get (:uniforms main-shader) "view")
          tex-location (get (:uniforms main-shader) "tex")
          tex-scale-location (get (:uniforms main-shader) "tex_scale")
          model-matrix-location (get (:uniforms main-shader) "model")]
      (debug-println (:uniforms main-shader))
      (debug-println (:attrs main-shader))
      (when (< projection-matrix-location 0) (throw (Exception. "projection-matrix-location is invalid!")))
      (when (< view-matrix-location 0) (throw (Exception. "view-matrix-location is invalid!")))
      (when (< model-matrix-location 0) (throw (Exception. "model-matrix-location is invalid!")))
      (bind-texture tex 0)
      (use-program main-shader)
      (set-uniform-value projection-matrix-location projection-matrix)
      (set-uniform-value view-matrix-location (Matrix4f.))
      (set-uniform-value tex-location 0)
      (set-uniform-value model-matrix-location (scale-matrix (Matrix4f.) (Vector3f. 10 10 10)))
      (set-uniform-value tex-scale-location 10.0)
      (let [vtx-coord-location (get (:attrs main-shader) "Position")
            txc-coord-location (get (:attrs main-shader) "in_tex_coord")
            mesh (load-obj-file "models/texcube.obj" vtx-coord-location txc-coord-location)]
            ;; base-mesh (create-mesh vtx-coord-location
            ;;                        (float-array [0.0 0.0 0.0
            ;;                                      1.0 0.0 0.0
            ;;                                      0.0 1.0 0.0
            ;;                                      1.0 1.0 0.0])
            ;;                        (int-array [0 3 1 0 3 2])
            ;;                        GL11/GL_TRIANGLES)
            ;; tcbuf-id (GL15/glGenBuffers)
            ;; buf (to-floatbuffer (float-array [0.0 1.0
            ;;                                   1.0 1.0
            ;;                                   0.0 0.0
            ;;                                   1.0 0.0]))
            ;; mesh (do
            ;;        (with-mesh base-mesh
            ;;          (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER tcbuf-id)
            ;;          (GL15/glBufferData GL15/GL_ARRAY_BUFFER buf GL15/GL_STATIC_DRAW)
            ;;          (set-vertex-attrib-pointer txc-coord-location 2 GL11/GL_FLOAT false 0))
            ;;        (assoc base-mesh :tcbuf tcbuf-id))]

        (debug-println "vtx-coord-location =" vtx-coord-location)
        (debug-println "txc-coord-location =" txc-coord-location)
        (debug-println "mesh =" mesh)

        (defn render-loop [state]
          (recur (synced-frame 60
                               (let [new-state (process-input state)
                                     pos (:pos new-state)
                                     h-angle (:h-angle new-state)
                                     v-angle (:v-angle new-state)
                                     mouse-grab (:mouse-grab new-state)
                                     debug (:debug new-state)]
                                 (Mouse/setGrabbed mouse-grab)
                                 (set-global-debug debug)
                                 (use-program main-shader)
                                 (bind-texture tex 0)
                                 (let [
                                       view-matrix-inv-t (translate-matrix base-transform (negate-vector3f pos))
                                       view-matrix-h (rotate-matrix view-matrix-inv-t (- h-angle) y-axis)
                                       view-matrix (rotate-matrix view-matrix-h (- v-angle) x-axis)
                                       ;view-matrix (translate-matrix view-matrix-r pos)
]
                                   (set-uniform-value view-matrix-location view-matrix)
                                   (with-mesh mesh
                                     (draw-mesh mesh)))
                                 new-state))))
        (render-loop (make-state pos true))

        ))))

(defn -main
  "Run a simple demo"
  [& args]
  (run-test))
