(ns clojure-game-kit.texture
  (:import (org.lwjgl LWJGLException)
           (org.lwjgl.opengl GL11 GL13 GL15 GL20 GL30)
           (org.lwjgl BufferUtils)
           (java.nio.file Path)
           (org.impulse101.jimgload IMGLoad))
  (:require [clojure-game-kit.util :refer :all])
  (:gen-class))

(defn apply-texture-parameters [tex parameter-map]
  (doseq [param parameter-map]
    (let [param-name (key param)
          param-value (val param)]
      (GL11/glTexParameteri tex param-name param-value))))

(defn bind-texture [texture index]
  (GL13/glActiveTexture (+ GL13/GL_TEXTURE0 index))
  (GL11/glBindTexture GL11/GL_TEXTURE_2D (:id texture)))

(defn unbind-texture [index]
  (GL13/glActiveTexture (+ GL13/GL_TEXTURE0 index))
  (GL11/glBindTexture GL11/GL_TEXTURE_2D 0))

(defn- setup-texture-2d! [pixel-buffer
                          texture-unit
                          tex-id
                          width
                          height
                          pixel-format
                          pixel-alignment
                          min-filter
                          mag-filter
                          s-config
                          t-config]
  (GL13/glActiveTexture texture-unit)
  (GL11/glBindTexture GL11/GL_TEXTURE_2D tex-id)
  (apply-texture-parameters GL11/GL_TEXTURE_2D
                            {GL11/GL_TEXTURE_MIN_FILTER min-filter
                             GL11/GL_TEXTURE_MAG_FILTER mag-filter
                             GL11/GL_TEXTURE_WRAP_S s-config
                             GL11/GL_TEXTURE_WRAP_T t-config})
  (GL11/glPixelStorei GL11/GL_UNPACK_ALIGNMENT pixel-alignment)
  (GL11/glTexImage2D GL11/GL_TEXTURE_2D 0 GL11/GL_RGBA
                     width height 0 pixel-format
                     GL11/GL_UNSIGNED_BYTE pixel-buffer)
  (case+ min-filter
         GL11/GL_NEAREST_MIPMAP_NEAREST (GL30/glGenerateMipmap GL11/GL_TEXTURE_2D)
         GL11/GL_LINEAR_MIPMAP_NEAREST (GL30/glGenerateMipmap GL11/GL_TEXTURE_2D)
         GL11/GL_NEAREST_MIPMAP_LINEAR (GL30/glGenerateMipmap GL11/GL_TEXTURE_2D)
         GL11/GL_LINEAR_MIPMAP_LINEAR (GL30/glGenerateMipmap GL11/GL_TEXTURE_2D)
         nil))

(defn make-texture-2d [pixel-buffer
                       width
                       height
                       pixel-format
                       pixel-alignment
                       texture-unit
                       min-filter
                       mag-filter
                       s-config
                       t-config]
  (let [tex-id (GL11/glGenTextures)]
    (setup-texture-2d! pixel-buffer
                       texture-unit
                       tex-id
                       width
                       height
                       pixel-format
                       pixel-alignment
                       min-filter
                       mag-filter
                       s-config
                       t-config)
    {:id tex-id
     :width width
     :height height
     :min-filter min-filter
     :mag-filter mag-filter
     :s-config s-config
     :t-config t-config
     :path nil}))

(defn load-texture-2d [path texture-unit min-filter mag-filter s-config t-config]
  (let [tex-desc (IMGLoad/loadImage path)
        bytes (.getBytes tex-desc)
        width (.getWidth tex-desc)
        height (.getHeight tex-desc)
        bpp (.getBPP tex-desc)
        tex (make-texture-2d bytes
                             width
                             height
                             (case bpp
                               24 GL11/GL_RGB
                               32 GL11/GL_RGBA
                               (throw (Exception. (format "Bad BPP: %1$d" bpp))))
                             1
                             texture-unit
                             min-filter
                             mag-filter
                             s-config
                             t-config)]
     (assoc tex :path path)))

(defn reload-texture-2d [texture-map texture-unit]
  (let [{:keys [path id min-filter mag-filter s-config t-config]} texture-map
        tex-desc (IMGLoad/loadImage path)
        bytes (.getBytes tex-desc)
        width (.getWidth tex-desc)
        height (.getHeight tex-desc)
        bpp (.getBPP tex-desc)]
    ;TODO: test bpp, too.
    (if (and (= width (:width texture-map))
             (= height (:height texture-map)))
      (do
        ;Reuse existing GL texture
        (setup-texture-2d! texture-unit
                           id
                           bytes
                           width
                           height
                           (case bpp
                             24 GL11/GL_RGB
                             32 GL11/GL_RGBA
                             (throw (Exception. (format "Bad BPP: %1$d" bpp))))
                           1
                           texture-unit
                           min-filter
                           mag-filter
                           s-config
                           t-config)
        texture-map)
      (do
        ;Replace existing GL texture
        (GL11/glDeleteTextures id)
        (load-texture-2d path
                         texture-unit
                         min-filter
                         mag-filter
                         s-config
                         t-config)))))

(defn replace-texture-2d [texture-map path texture-unit]
  (reload-texture-2d (assoc texture-map :path path) texture-unit))
