(ns clojure-game-kit.shader
  (:import (org.lwjgl LWJGLException)
           (org.lwjgl.opengl GL11 GL15 GL20 GL30)
           (java.nio.file Path)
           (org.lwjgl.util.vector Matrix4f))
  (:require [clojure-game-kit.util :refer :all]
            [clojure-game-kit.vbo :refer :all])
  (:gen-class))

(defmacro make-program [] `(GL20/glCreateProgram))

(defn make-shader [shader-type]
  (GL20/glCreateShader (case shader-type
                         :vertex GL20/GL_VERTEX_SHADER
                         :fragment GL20/GL_FRAGMENT_SHADER)))

(defn make-vertex-shader []
  (make-shader :vertex))

(defn make-fragment-shader []
  (make-shader :fragment))

(defn compile-shader [shader-id source-text]
  (GL20/glShaderSource shader-id source-text)
  (GL20/glCompileShader shader-id)
  (let [result (= (GL20/glGetShaderi shader-id GL20/GL_COMPILE_STATUS) GL11/GL_TRUE)
        log (GL20/glGetShaderInfoLog shader-id 8192)]
    (when (not result) (throw (Exception. log)))))

(defmacro attach-shader [program-id shader-id] `(GL20/glAttachShader ~program-id ~shader-id))
(defmacro detach-shader [program-id shader-id] `(GL20/glDetachShader ~program-id ~shader-id))
(defmacro delete-shader [shader-id] `(GL20/glDeleteShader ~shader-id))
(defmacro delete-program [program-id] `(GL20/glDeleteProgram ~program-id))

(defn link-program [program-id]
  (GL20/glLinkProgram program-id)
  (let [result (= (GL20/glGetProgrami program-id GL20/GL_LINK_STATUS) GL11/GL_TRUE)
        log (GL20/glGetProgramInfoLog program-id 8192)]
    (when (not result) (throw (Exception. log)))))

(defn destroy-program [program vtx frag]
  (delete-shader vtx)
  (delete-shader frag)
  (delete-program program))

(defn set-attr-location [program-id attrib location]
  (debug-println "Setting " attrib " to index " location)
  (GL20/glBindAttribLocation program-id location attrib))

(defn set-attr-locations [program-id attr-map]
  (doseq [attr-entry attr-map]
    (let [attr-name (key attr-entry)
          attr-index (val attr-entry)]
      (debug-println "Setting " attr-name " to index " attr-index)
      (set-attr-location program-id attr-name attr-index))))

(defn get-attr-location [program-id attrib]
  (GL20/glGetAttribLocation program-id attrib))

(defn get-uniform-location [program-id uniform]
  (let [result (GL20/glGetUniformLocation program-id uniform)]
    (debug-println program-id ":" uniform ":" result)
    result))

(defn get-uniform-locations [program-id uniform-names]
  (reduce #(assoc %1 %2 (get-uniform-location program-id %2))
          {}
          uniform-names))

(defn make-complete-program [vertex-source fragment-source uniform-list attr-map]
  (let [program-id (make-program)
        vtx (make-vertex-shader)
        frag (make-fragment-shader)]
    (try
      (compile-shader vtx vertex-source)
      (compile-shader frag fragment-source)
      (attach-shader program-id vtx)
      (attach-shader program-id frag)
      (set-attr-locations program-id attr-map)
      (link-program program-id)
      {:prog program-id
       :vtx vtx
       :frag frag
       :uniforms (get-uniform-locations program-id uniform-list)
       :attrs attr-map}
      (catch Exception e (destroy-program program-id vtx frag) (throw e)))))

(defn use-program [program-map]
  (GL20/glUseProgram (:prog program-map)))

(defn unuse-program []
  (GL20/glUseProgram 0))

(defn update-program [program-map vertex-source fragment-source uniform-list attr-map]
  (let [program-id (:prog program-map)
        old-vertex-id (:vtx program-map)
        old-fragment-id (:frag program-map)
        new-vertex-id (if vertex-source (make-vertex-shader) old-vertex-id)
        new-fragment-id (if fragment-source (make-fragment-shader) old-fragment-id)]

    (when vertex-source
      (let [new-vertex-result (compile-shader new-vertex-id vertex-source)]
        (when (not (:result new-vertex-result))
          (throw (Exception. (:log new-vertex-result))))))
    (when fragment-source
      (let [new-fragment-result (compile-shader new-fragment-id fragment-source)]
        (when (not (:result new-fragment-result))
          (throw (Exception. (:log new-fragment-result))))))

    (when old-vertex-id
      (do
        (detach-shader program-id old-vertex-id)
        (delete-shader old-vertex-id)))
    (when new-vertex-id
        (attach-shader program-id new-vertex-id))
    (when old-fragment-id
      (do
        (detach-shader program-id old-fragment-id)
        (delete-shader old-fragment-id)))
    (when new-fragment-id
      (attach-shader program-id new-fragment-id))

    (set-attr-locations program-id attr-map)
    (link-program program-id new-vertex-id new-fragment-id)

    (let [uniform-map (get-uniform-locations program-id uniform-list)]
      {:prog program-id
       :vtx new-vertex-id
       :frag new-fragment-id
       :uniforms uniform-map
       :attrs attr-map})))

(defn load-shader [shader-dir]
  (let [vtx-file (join-paths shader-dir "vert.glsl")
        frag-file (join-paths shader-dir "frag.glsl")
        uniform-list-file (join-paths shader-dir "uniforms.list")
        attr-map-file (join-paths shader-dir "attributes.map")
        vtx-source (slurp (open-file vtx-file))
        frag-source (slurp (open-file frag-file))
        uniform-list (load-reader (open-file uniform-list-file))
        attr-map (load-reader (open-file attr-map-file))]
    (make-complete-program vtx-source frag-source uniform-list attr-map)))

(def long-type (Class/forName "java.lang.Long"))
(def double-type (Class/forName "java.lang.Double"))

(defmulti set-uniform-value (fn [uniform-location value] (type value)))
(defmethod set-uniform-value Matrix4f [uniform-location value]
  (debug-println "set-uniform-value Matrix4f" uniform-location "\n" value)
  (GL20/glUniformMatrix4 uniform-location false (to-floatbuffer value)))
(defmethod set-uniform-value long-type [uniform-location value]
  (debug-println "set-uniform-value long" uniform-location value)
  (GL20/glUniform1i uniform-location value))
(defmethod set-uniform-value double-type [uniform-location value]
  (debug-println "set-uniform-value long" uniform-location value)
  (GL20/glUniform1f uniform-location value))

(defn set-vertex-attrib-pointer [index size type normalized? stride]
  (debug-println "set-vertex-attrib-pointer" index size type normalized? stride)
  (GL20/glVertexAttribPointer index size type normalized? stride 0)
  (GL20/glEnableVertexAttribArray index))
