(ns clojure-game-kit.vbo
  (:import (java.nio ByteBuffer FloatBuffer)
           (org.lwjgl LWJGLException BufferUtils)
           (org.lwjgl.opengl GL11 GL15 GL20 GL30)
           (org.lwjgl.util.vector Matrix4f Vector3f))
  (:gen-class))

(defn make-vbo []
  (GL15/glGenBuffers))

(defn bind-vbo [vbo-id]
  (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER vbo-id))

(defn unbind-vbo []
  (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER 0))

(defmacro with-vbo [vbo-id & body]
  `(do
     (bind-vbo ~vbo-id)
     (let [result# (do ~@body)]
       (unbind-vbo)
       result#)))

(defn upload-floats [#^floats data]
  (let [buf (BufferUtils/createFloatBuffer (alength data))]
    (.put buf data)
    (.flip buf)
    (GL15/glBufferData GL15/GL_ARRAY_BUFFER buf GL15/GL_STATIC_DRAW)))

(defn setup-float-attr-pointer [location size normalized? stride]
  (GL20/glVertexAttribPointer location size GL11/GL_FLOAT normalized? stride 0))

(defn make-ibo []
  (GL15/glGenBuffers))

(defn bind-ibo [ibo-id]
  (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER ibo-id))

(defn unbind-ibo []
  (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER 0))

(defmacro with-ibo [ibo-id & body]
  `(do
     (bind-ibo ~ibo-id)
     (let [result# (do ~@body)]
       (unbind-ibo)
       result#)))

(defn upload-int-elements [#^ints data]
  (let [buf (BufferUtils/createIntBuffer (alength data))]
    (.put buf data)
    (.flip buf)
    (GL15/glBufferData GL15/GL_ELEMENT_ARRAY_BUFFER buf GL15/GL_STATIC_DRAW)))

(defn make-vao []
  (GL30/glGenVertexArrays))

(defn bind-vao [vao-id]
  (GL30/glBindVertexArray vao-id))

(defn unbind-vao []
  (GL30/glBindVertexArray 0))

(defmacro with-vao [vao-id & body]
  `(do
     (bind-vao ~vao-id)
     (let [result# (do ~@body)]
       (unbind-vao)
       result#)))

(def float-array-type (Class/forName "[F"))

(defmulti to-floatbuffer class)
(defmethod to-floatbuffer Matrix4f [c]
  (let [buf (BufferUtils/createFloatBuffer 16)]
    (.store c buf)
    (.flip buf)
    buf))
(defmethod to-floatbuffer float-array-type [c]
  (let [buf (BufferUtils/createFloatBuffer (alength c))]
    (.put buf c)
    (.flip buf)
    buf))
