/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codeberg.anari.api;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import java.util.function.BiFunction;

/**
 * Geometries in ANARI are objects that describe the spatial representation of a
 * surface. Geometries are typically limited to a maximum of 2^32 primitives.
 *
 * {@snippet lang=c :
 * typedef void* ANARIGeometry;
 * }
 *
 * @author Johann Sorel
 */
public class Geometry<T extends Geometry<T>> extends Object<T> {

    public static class SubType<T extends Geometry> extends Object.SubType<T>{

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<Cone> CONE = new SubType<>("cone",Cone::new);
        public static final SubType<Curve> CURVE = new SubType<>("curve",Curve::new);
        public static final SubType<Cylinder> CYLINDER = new SubType<>("cylinder",Cylinder::new);
        public static final SubType<Isosurface> ISOSURFACE = new SubType<>("isosurface",Isosurface::new);
        public static final SubType<Quad> QUAD = new SubType<>("quad",Quad::new);
        public static final SubType<Sphere> SPHERE = new SubType<>("sphere",Sphere::new);
        public static final SubType<Triangle> TRIANGLE = new SubType<>("triangle",Triangle::new);
    }

    /**
     * Set the uniform color attribute, used when no per-primitive or per-vertex
     * color is set. Unspecified components default to zero for the first three
     * components and to one for the fourth component.
     */
    public T setColor(float r, float g, float b, float a) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Geometry.PARAM_COLOR, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, r, g, b, a));
        }
    }

    /**
     * Set the uniform attribute 0, used when no per-primitive or per-vertex
     * attribute0 is set. Unspecified components default to zero for the first
     * three components and to one for the fourth component.
     */
    public T setAttribute0(float x, float y, float z, float w) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Geometry.PARAM_ATTRIBUTE0, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Set the uniform attribute 1, used when no per-primitive or per-vertex
     * attribute1 is set. Unspecified components default to zero for the first
     * three components and to one for the fourth component.
     */
    public T setAttribute1(float x, float y, float z, float w) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Geometry.PARAM_ATTRIBUTE1, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Set the uniform attribute 2, used when no per-primitive or per-vertex
     * attribute2 is set. Unspecified components default to zero for the first
     * three components and to one for the fourth component.
     */
    public T setAttribute2(float x, float y, float z, float w) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Geometry.PARAM_ATTRIBUTE2, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Set the uniform attribute 3, used when no per-primitive or per-vertex
     * attribute3 is set. Unspecified components default to zero for the first
     * three components and to one for the fourth component.
     */
    public T setAttribute3(float x, float y, float z, float w) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Geometry.PARAM_ATTRIBUTE3, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Set the per-primitive color attribute. Takes precedence over the uniform color.
     */
    public T setPrimitiveColor(Array1D array) throws Throwable {
        return setObject(Properties.Geometry.PARAM_PRIMITIVE_COLOR, DataType.ARRAY1D, array);
    }

    /**
     * Set the per-primitive attribute 0. Takes precedence over the uniform attribute0.
     */
    public T setPrimitiveAttribute0(Array1D array) throws Throwable {
        return setObject(Properties.Geometry.PARAM_PRIMITIVE_ATTRIBUTE0, DataType.ARRAY1D, array);
    }

    /**
     * Set the per-primitive attribute 1. Takes precedence over the uniform attribute1.
     */
    public T setPrimitiveAttribute1(Array1D array) throws Throwable {
        return setObject(Properties.Geometry.PARAM_PRIMITIVE_ATTRIBUTE1, DataType.ARRAY1D, array);
    }

    /**
     * Set the per-primitive attribute 2. Takes precedence over the uniform attribute2.
     */
    public T setPrimitiveAttribute2(Array1D array) throws Throwable {
        return setObject(Properties.Geometry.PARAM_PRIMITIVE_ATTRIBUTE2, DataType.ARRAY1D, array);
    }

    /**
     * Set the per-primitive attribute 3. Takes precedence over the uniform attribute3.
     */
    public T setPrimitiveAttribute3(Array1D array) throws Throwable {
        return setObject(Properties.Geometry.PARAM_PRIMITIVE_ATTRIBUTE3, DataType.ARRAY1D, array);
    }

    /**
     * Set the per-primitive id array, exposed as the primitiveId attribute.
     */
    public T setPrimitiveId(Array1D array) throws Throwable {
        return setObject(Properties.Geometry.PARAM_PRIMITIVE_ID, DataType.ARRAY1D, array);
    }

    public Geometry(Device device, String subtype, MemorySegment pointer) {
        super(device, DataType.GEOMETRY, subtype, pointer);
    }

    /**
     * A geometry consisting of individual cones.
     * <br>
     * Extension KHR_GEOMETRY_CONE
     */
    public static class Cone extends Geometry<Cone> {

        public Cone(Device device, MemorySegment pointer) {
            super(device, SubType.CONE.name, pointer);
        }

        /**
         * Required vertex positions of the cones.
         */
        public Cone setVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Radius at each vertex.
         */
        public Cone setVertexRadius(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_RADIUS, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex end cap flags (0 means no caps, 1 means flat-capped).
         */
        public Cone setVertexCap(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_CAP, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex color.
         */
        public Cone setVertexColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 0.
         */
        public Cone setVertexAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 1.
         */
        public Cone setVertexAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 2.
         */
        public Cone setVertexAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 3.
         */
        public Cone setVertexAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Optional indices into the vertex.* arrays, each pair defines one cone.
         * If not given, a "cone soup" is assumed, i.e. each two consecutive vertices form one cone.
         */
        public Cone setPrimitiveIndex(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_PRIMITIVE_INDEX, DataType.ARRAY1D, array);
        }

        /**
         * Default vertex caps for all cones if vertex.cap is not set,
         * possible values: none, first, second, both. Default: none.
         */
        public Cone setCaps(String caps) throws Throwable {
            return setString(Properties.Geometry.PARAM_CAPS, caps);
        }
    }

    /**
     * A geometry consisting of multiple curves. The vertices of the curve(s) are
     * connected by linear, round segments (i.e. cones or cylinders).
     * <br>
     * Extension KHR_GEOMETRY_CURVE
     */
    public static class Curve extends Geometry<Curve> {

        public Curve(Device device, MemorySegment pointer) {
            super(device, SubType.CURVE.name, pointer);
        }

        /**
         * Required vertex positions.
         */
        public Curve setVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Radius at each vertex.
         */
        public Curve setVertexRadius(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_RADIUS, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex color.
         */
        public Curve setVertexColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 0.
         */
        public Curve setVertexAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 1.
         */
        public Curve setVertexAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 2.
         */
        public Curve setVertexAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 3.
         */
        public Curve setVertexAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Optional indices into vertex.* arrays, each index defines the start of one
         * segment of a curve (the end is implicitly given with index+1). If not given,
         * a single curve is assumed, connecting all vertices.
         */
        public Curve setPrimitiveIndex(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_PRIMITIVE_INDEX, DataType.ARRAY1D, array);
        }

        /**
         * Default radius for all curve vertices (if vertex.radius is not set). Default: 1.
         */
        public Curve setRadius(float radius) throws Throwable {
            return setFloat32(Properties.Geometry.PARAM_RADIUS, radius);
        }
    }

    /**
     * A geometry consisting of individual cylinders, each of which can have an own radius.
     * <br>
     * Extension KHR_GEOMETRY_CYLINDER
     */
    public static class Cylinder extends Geometry<Cylinder> {

        public Cylinder(Device device, MemorySegment pointer) {
            super(device, SubType.CYLINDER.name, pointer);
        }

        /**
         * Required vertex positions.
         */
        public Cylinder setVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex end cap flags (0 means no caps, 1 means flat-capped).
         */
        public Cylinder setVertexCap(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_CAP, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex color.
         */
        public Cylinder setVertexColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 0.
         */
        public Cylinder setVertexAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 1.
         */
        public Cylinder setVertexAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 2.
         */
        public Cylinder setVertexAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 3.
         */
        public Cylinder setVertexAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Optional indices into vertex.* arrays, each pair defines one cylinder.
         * If not given, a "cylinder soup" is assumed, i.e. each two consecutive
         * vertices form one cylinder.
         */
        public Cylinder setPrimitiveIndex(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_PRIMITIVE_INDEX, DataType.ARRAY1D, array);
        }

        /**
         * Per-cylinder radius.
         */
        public Cylinder setPrimitiveRadius(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_PRIMITIVE_RADIUS, DataType.ARRAY1D, array);
        }

        /**
         * Default radius for all cylinders (if primitive.radius is not set). Default: 1.
         */
        public Cylinder setRadius(float radius) throws Throwable {
            return setFloat32(Properties.Geometry.PARAM_RADIUS, radius);
        }

        /**
         * Default vertex caps for all cylinders if vertex.cap is not set,
         * possible values: none, first, second, both. Default: none.
         */
        public Cylinder setCaps(String caps) throws Throwable {
            return setString(Properties.Geometry.PARAM_CAPS, caps);
        }
    }

    /**
     * A geometry defined implicitly as the surface(s) of constant value of a spatial field.
     * <br>
     * Extension KHR_GEOMETRY_ISOSURFACE
     */
    public static class Isosurface extends Geometry<Isosurface> {

        public Isosurface(Device device, MemorySegment pointer) {
            super(device, SubType.ISOSURFACE.name, pointer);
        }

        /**
         * Single isovalue defining the isosurface. Must be set (either this or the
         * array variant) together with field to yield a valid isosurface geometry.
         */
        public Isosurface setIsovalue(float isovalue) throws Throwable {
            return setFloat32(Properties.Geometry.PARAM_ISOVALUE, isovalue);
        }

        /**
         * Array of isovalues defining multiple isosurfaces, which become the
         * primitives of this isosurface geometry. Must be set (either this or the
         * single-value variant) together with field to yield a valid isosurface geometry.
         */
        public Isosurface setIsovalue(Array1D isovalues) throws Throwable {
            return setObject(Properties.Geometry.PARAM_ISOVALUE, DataType.ARRAY1D, isovalues);
        }

        /**
         * Spatial field to be isosurfaced. Required together with isovalue to yield
         * a valid isosurface geometry.
         */
        public Isosurface setField(SpatialField field) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FIELD, DataType.SPATIAL_FIELD, field);
        }
    }

    /**
     * A geometry consisting of quads.
     * <br>
     * Extension KHR_GEOMETRY_QUAD
     */
    public static class Quad extends Geometry<Quad> {

        public Quad(Device device, MemorySegment pointer) {
            super(device, SubType.QUAD.name, pointer);
        }

        /**
         * Required vertex positions, must contain at least four elements.
         */
        public Quad setVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Vertex normals.
         */
        public Quad setVertexNormal(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_NORMAL, DataType.ARRAY1D, array);
        }

        /**
         * Vertex tangents. The fourth component (if present) indicates the
         * handedness of the tangent space coordinate system and must be 1 or -1.
         */
        public Quad setVertexTangent(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_TANGENT, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex color.
         */
        public Quad setVertexColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 0.
         */
        public Quad setVertexAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 1.
         */
        public Quad setVertexAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 2.
         */
        public Quad setVertexAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 3.
         */
        public Quad setVertexAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying normals, mapping unique values to each indexed primitive
         * vertex. Takes precedence over vertex.normal when both are present.
         */
        public Quad setFaceVaryingNormal(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_NORMAL, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying tangents, mapping unique values to each indexed primitive
         * vertex. Takes precedence over vertex.tangent when both are present.
         */
        public Quad setFaceVaryingTangent(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_TANGENT, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying colors, mapping unique values to each indexed primitive
         * vertex. Takes precedence over vertex.color when both are present.
         */
        public Quad setFaceVaryingColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 0. Takes precedence over vertex.attribute0 when both are present.
         */
        public Quad setFaceVaryingAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 1. Takes precedence over vertex.attribute1 when both are present.
         */
        public Quad setFaceVaryingAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 2. Takes precedence over vertex.attribute2 when both are present.
         */
        public Quad setFaceVaryingAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 3. Takes precedence over vertex.attribute3 when both are present.
         */
        public Quad setFaceVaryingAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Optional indices (into the vertex array(s)), each 4-tuple defines one quad.
         * If not given, a "quad soup" is assumed, i.e. each four consecutive vertices
         * form one quad.
         */
        public Quad setPrimitiveIndex(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_PRIMITIVE_INDEX, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed vertex position arrays for deformation motion blur.
         * <br>
         * Extension KHR_GEOMETRY_QUAD_MOTION_DEFORMATION
         */
        public Quad setMotionVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_MOTION_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed vertex normal arrays for deformation motion blur.
         * <br>
         * Extension KHR_GEOMETRY_QUAD_MOTION_DEFORMATION
         */
        public Quad setMotionVertexNormal(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_MOTION_VERTEX_NORMAL, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed vertex tangent arrays for deformation motion blur.
         * <br>
         * Extension KHR_GEOMETRY_QUAD_MOTION_DEFORMATION
         */
        public Quad setMotionVertexTangent(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_MOTION_VERTEX_TANGENT, DataType.ARRAY1D, array);
        }

        /**
         * Time (start, end) associated with the first and last key in the motion.*
         * arrays, in combination with extension KHR_CAMERA_SHUTTER. Default: [0, 1].
         * <br>
         * Extension KHR_GEOMETRY_QUAD_MOTION_DEFORMATION
         */
        public Quad setTime(float start, float end) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Geometry.PARAM_TIME, DataType.FLOAT32_BOX1, tempArena.allocateFrom(JAVA_FLOAT, start, end));
            }
        }
    }

    /**
     * A geometry consisting of individual spheres, each of which can have an own radius.
     * <br>
     * Extension KHR_GEOMETRY_SPHERE
     */
    public static class Sphere extends Geometry<Sphere> {

        public Sphere(Device device, MemorySegment pointer) {
            super(device, SubType.SPHERE.name, pointer);
        }

        /**
         * Required center positions of the spheres.
         */
        public Sphere setVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Per-sphere radius.
         */
        public Sphere setVertexRadius(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_RADIUS, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex color.
         */
        public Sphere setVertexColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 0.
         */
        public Sphere setVertexAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 1.
         */
        public Sphere setVertexAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 2.
         */
        public Sphere setVertexAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 3.
         */
        public Sphere setVertexAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Optional indices (into the vertex array(s)). If not given, a "sphere soup"
         * is assumed, using all spheres at vertex.position.
         */
        public Sphere setPrimitiveIndex(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_PRIMITIVE_INDEX, DataType.ARRAY1D, array);
        }

        /**
         * Default radius for all spheres (if vertex.radius is not set). Default: 1.
         */
        public Sphere setRadius(float radius) throws Throwable {
            return setFloat32(Properties.Geometry.PARAM_RADIUS, radius);
        }

    }

    /**
     * A geometry consisting of triangles.
     * <br>
     * Extension KHR_GEOMETRY_TRIANGLE
     */
    public static class Triangle extends Geometry<Triangle> {

        public Triangle(Device device, MemorySegment pointer) {
            super(device, SubType.TRIANGLE.name, pointer);
        }

        /**
         * Required vertex positions, must contain at least three elements.
         */
        public Triangle setVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Vertex normals.
         */
        public Triangle setVertexNormal(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_NORMAL, DataType.ARRAY1D, array);
        }

        /**
         * Vertex tangents. The fourth component (if present) indicates the
         * handedness of the tangent space coordinate system and must be 1 or -1.
         */
        public Triangle setVertexTangent(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_TANGENT, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex color.
         */
        public Triangle setVertexColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 0.
         */
        public Triangle setVertexAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 1.
         */
        public Triangle setVertexAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 2.
         */
        public Triangle setVertexAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Per-vertex attribute 3.
         */
        public Triangle setVertexAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_VERTEX_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying normals, mapping unique values to each indexed primitive
         * vertex. Takes precedence over vertex.normal when both are present.
         */
        public Triangle setFaceVaryingNormal(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_NORMAL, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying tangents, mapping unique values to each indexed primitive
         * vertex. Takes precedence over vertex.tangent when both are present.
         */
        public Triangle setFaceVaryingTangent(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_TANGENT, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying colors, mapping unique values to each indexed primitive
         * vertex. Takes precedence over vertex.color when both are present.
         */
        public Triangle setFaceVaryingColor(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_COLOR, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 0. Takes precedence over vertex.attribute0 when both are present.
         */
        public Triangle setFaceVaryingAttribute0(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE0, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 1. Takes precedence over vertex.attribute1 when both are present.
         */
        public Triangle setFaceVaryingAttribute1(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE1, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 2. Takes precedence over vertex.attribute2 when both are present.
         */
        public Triangle setFaceVaryingAttribute2(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE2, DataType.ARRAY1D, array);
        }

        /**
         * Face-varying attribute 3. Takes precedence over vertex.attribute3 when both are present.
         */
        public Triangle setFaceVaryingAttribute3(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_FACEVARYING_ATTRIBUTE3, DataType.ARRAY1D, array);
        }

        /**
         * Optional indices (into the vertex array(s)), each 3-tuple defines one
         * triangle. If not given, a "triangle soup" is assumed, i.e. each three
         * consecutive vertices form one triangle.
         */
        public Triangle setPrimitiveIndex(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_PRIMITIVE_INDEX, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed vertex position arrays for deformation motion blur.
         * <br>
         * Extension KHR_GEOMETRY_TRIANGLE_MOTION_DEFORMATION
         */
        public Triangle setMotionVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_MOTION_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed vertex normal arrays for deformation motion blur.
         * <br>
         * Extension KHR_GEOMETRY_TRIANGLE_MOTION_DEFORMATION
         */
        public Triangle setMotionVertexNormal(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_MOTION_VERTEX_NORMAL, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed vertex tangent arrays for deformation motion blur.
         * <br>
         * Extension KHR_GEOMETRY_TRIANGLE_MOTION_DEFORMATION
         */
        public Triangle setMotionVertexTangent(Array1D array) throws Throwable {
            return setObject(Properties.Geometry.PARAM_MOTION_VERTEX_TANGENT, DataType.ARRAY1D, array);
        }

        /**
         * Time (start, end) associated with the first and last key in the motion.*
         * arrays, in combination with extension KHR_CAMERA_SHUTTER. Default: [0, 1].
         * <br>
         * Extension KHR_GEOMETRY_TRIANGLE_MOTION_DEFORMATION
         */
        public Triangle setTime(float start, float end) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Geometry.PARAM_TIME, DataType.FLOAT32_BOX1, tempArena.allocateFrom(JAVA_FLOAT, start, end));
            }
        }

    }
}
