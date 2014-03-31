(ns clojure-game-kit.opengl
  (:import (java.nio ByteBuffer FloatBuffer)
           (org.lwjgl LWJGLException BufferUtils)
           (org.lwjgl.opengl GL11 GL15 GL20 GL30 ContextAttribs PixelFormat Display DisplayMode ARBDebugOutput ARBDebugOutputCallback ARBDebugOutputCallback$Handler)
           (org.lwjgl.util.vector Matrix4f Vector3f))
  (:require [clojure-game-kit.vbo :refer :all]
            [clojure-game-kit.util :refer :all])
  (:gen-class))

(def debug-handler
  (reify ARBDebugOutputCallback$Handler
    (handleMessage [this source type id severity message]
      (printf "%d:%d:%d:%d %s\n" source type id severity message))))

(def x-axis (Vector3f. 1.0  0.0  0.0))
(def y-axis (Vector3f. 0.0  1.0  0.0))
(def z-axis (Vector3f. 0.0  0.0  1.0))

(defn opengl-attrs [& {:keys [major-version
                              minor-version
                              core-profile
                              debug-mode]
                       :or {major-version 3
                            minor-version 3
                            core-profile true
                            debug-mode false}}]
  {:major-version major-version
   :minor-version minor-version
   :core-profile core-profile
   :debug-mode debug-mode})

(defmacro with-opengl [display-width
                       display-height
                       window-title
                       attrs
                       & body]
  `(let [pixel-format# (PixelFormat.)
         {major-version# :major-version
          minor-version# :minor-version
          core-profile# :core-profile
          debug-mode# :debug-mode} ~attrs
         ctx-attribs# (.withDebug
                       (.withProfileCore
                        (ContextAttribs. major-version# minor-version#)
                        core-profile#)
                       debug-mode#)]
     (debug-println ctx-attribs#)
     (Display/setDisplayMode (DisplayMode. ~display-width ~display-height))
     (Display/setTitle ~window-title)
     (Display/create pixel-format# ctx-attribs#)
     (if debug-mode#
       (do
         (ARBDebugOutput/glDebugMessageCallbackARB (ARBDebugOutputCallback. debug-handler))
         (ARBDebugOutput/glDebugMessageControlARB GL11/GL_DONT_CARE GL11/GL_DONT_CARE GL11/GL_DONT_CARE nil true)
         (GL11/glEnable ARBDebugOutput/GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB)))
     (GL11/glViewport 0 0 ~display-width ~display-height)
     (GL11/glClearColor 0.1 0.1 0.1 0.0)
     (GL11/glEnable GL11/GL_DEPTH_TEST)
     (do
       ~@body)
     (Display/destroy)))

(defmacro sync-display [fps] `(Display/sync ~fps))

(defn clear-display []
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)))

(defmacro update-display [] `(Display/update))

(defmacro synced-frame [fps & body]
  `(try
     (when (> ~fps 0) (sync-display ~fps))
     (clear-display)
     ~@body
     (finally (update-display))))
