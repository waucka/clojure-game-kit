(ns clojure-game-kit.mesh
  (:import (java.nio ByteBuffer FloatBuffer)
           (org.lwjgl LWJGLException BufferUtils)
           (org.lwjgl.opengl GL11 GL15 GL20 GL30)
           (org.lwjgl.util.vector Matrix4f Vector3f)
           (jobjloader OBJLoader))
  (:require [clojure-game-kit.vbo :refer :all]
            [clojure-game-kit.shader :refer :all]
            [clojure-game-kit.util :refer :all])
  (:gen-class))

(defn create-mesh [vtx-coord-location #^floats vertices #^ints indices draw-type]
  (let [num-vertices (alength indices)
        vao-id (make-vao)
        vbo-id (make-vbo)
        ibo-id (make-ibo)]
    (with-vao vao-id
      (bind-ibo ibo-id)
      (upload-int-elements indices)
      (bind-vbo vbo-id)
      (upload-floats vertices)
      (set-vertex-attrib-pointer vtx-coord-location 3 GL11/GL_FLOAT false 0))
    (unbind-vbo)
    (unbind-ibo)
    {:vao vao-id
     :vbo vbo-id
     :ibo ibo-id
     :draw-type draw-type
     :num-vertices num-vertices
     :tcbuf nil}))

(defmacro with-mesh [mesh & body]
  `(with-vao (:vao ~mesh)
       ~@body))

(defn draw-mesh [mesh]
    (GL11/glDrawElements (:draw-type mesh) (:num-vertices mesh) GL11/GL_UNSIGNED_INT 0))

(defmacro into-vector [input-collection]
  `(into [] ~input-collection))

(defn load-obj-file [path vtx-coord-location tex-coord-location]
  (let [loader (OBJLoader.)]
    (.parse loader (open-file-for-reading path))
    (debug-println "vertices:" (into-vector (.getVertices loader)))
    (debug-println "indices:" (into-vector (.getIndices loader)))
    (debug-println "texcoords:" (into-vector (.getTexCoords loader)))
    (let [mesh (create-mesh vtx-coord-location (.getVertices loader) (.getIndices loader) GL11/GL_TRIANGLES)
          tcbuf-id (GL15/glGenBuffers)
          buf (to-floatbuffer (.getTexCoords loader))]
      (with-mesh mesh
        (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER tcbuf-id)
        (GL15/glBufferData GL15/GL_ARRAY_BUFFER buf GL15/GL_STATIC_DRAW)
        (set-vertex-attrib-pointer tex-coord-location 2 GL11/GL_FLOAT false 0)
        (assoc mesh :tcbuf tcbuf-id)))))
