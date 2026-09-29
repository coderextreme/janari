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
 * ANARI sampler objects map attribute data into other object inputs such as
 * materials. The sampled value is completed to four components (unspecified
 * components default to zero for the first three, one for the fourth),
 * multiplied by outTransform and added to outOffset to yield the final result.
 *
 * {@snippet lang=c :
 * typedef void* ANARISampler;
 * }
 *
 * @author Johann Sorel
 */
public class Sampler<T extends Sampler<T>> extends Object<T> {

    public static class SubType<T extends Sampler> extends Object.SubType<T>{

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<Image1D> IMAGE1D = new SubType<>("image1D",Image1D::new);
        public static final SubType<Image2D> IMAGE2D = new SubType<>("image2D",Image2D::new);
        public static final SubType<Image3D> IMAGE3D = new SubType<>("image3D",Image3D::new);
        public static final SubType<Primitive> PRIMITIVE = new SubType<>("primitive",Primitive::new);
        public static final SubType<Transform> TRANSFORM = new SubType<>("transform",Transform::new);
    }

    public Sampler(Device device, String subtype, MemorySegment pointer) {
        super(device, DataType.SAMPLER, subtype, pointer);
    }

    /**
     * A one dimensional image sampler.
     * <br>
     * Extension KHR_SAMPLER_IMAGE1D
     */
    public static class Image1D extends Sampler<Image1D> {

        public Image1D(Device device, MemorySegment pointer) {
            super(device, SubType.IMAGE1D.name, pointer);
        }

        /**
         * Surface attribute used as texture coordinate. Default: attribute0.
         */
        public Image1D setInAttribute(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_INATTRIBUTE, value);
        }

        /**
         * Transform applied to the input attribute before sampling
         * (column-major, applied as inTransform * x + inOffset). Default: identity.
         */
        public Image1D setInTransform(float[] matrix4x4) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_INTRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Offset added to the input transform result. Default: (0, 0, 0, 0).
         */
        public Image1D setInOffset(float[] offset) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_INOFFSET, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, offset));
            }
        }

        /**
         * Array backing the sampler.
         */
        public Image1D setImage(Array1D value) throws Throwable {
            return setObject(Properties.Sampler.PARAM_IMAGE, DataType.ARRAY1D, value);
        }

        /**
         * Filter of the sampler, possible values: nearest, linear. Default: linear.
         */
        public Image1D setFilter(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_FILTER, value);
        }

        /**
         * Wrap mode of the sampler, possible values: clampToEdge, repeat,
         * mirrorRepeat. Default: clampToEdge.
         */
        public Image1D setWrapMode(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_WRAPMODE, value);
        }

        /**
         * Transform applied to the sampled values, column-major, applied to
         * column vectors (outTransform * x + outOffset). Default: identity.
         */
        public Image1D setOutTransform(float[] matrix4x4) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTTRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Offset added to the output transform result. Default: (0, 0, 0, 0).
         */
        public Image1D setOutOffset(float[] offset) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTOFFSET, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, offset));
            }
        }
    }

    /**
     * A two dimensional image sampler.
     * <br>
     * Extension KHR_SAMPLER_IMAGE2D
     */
    public static class Image2D extends Sampler<Image2D> {

        public Image2D(Device device, MemorySegment pointer) {
            super(device, SubType.IMAGE2D.name, pointer);
        }

        /**
         * Surface attribute used as texture coordinate. Default: attribute0.
         */
        public Image2D setInAttribute(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_INATTRIBUTE, value);
        }

        /**
         * Transform applied to the input attribute before sampling. Default: identity.
         */
        public Image2D setInTransform(float[] matrix4x4) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_INTRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Offset added to the input transform result. Default: (0, 0, 0, 0).
         */
        public Image2D setInOffset(float[] offset) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_INOFFSET, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, offset));
            }
        }

        /**
         * Array backing the sampler.
         */
        public Image2D setImage(Array2D value) throws Throwable {
            return setObject(Properties.Sampler.PARAM_IMAGE, DataType.ARRAY2D, value);
        }

        /**
         * Filter of the sampler, possible values: nearest, linear. Default: linear.
         */
        public Image2D setFilter(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_FILTER, value);
        }

        /**
         * Wrap mode of the sampler for the 1st dimension, possible values:
         * clampToEdge, repeat, mirrorRepeat. Default: clampToEdge.
         */
        public Image2D setWrapMode1(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_WRAPMODE1, value);
        }

        /**
         * Wrap mode of the sampler for the 2nd dimension, possible values:
         * clampToEdge, repeat, mirrorRepeat. Default: clampToEdge.
         */
        public Image2D setWrapMode2(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_WRAPMODE2, value);
        }

        /**
         * Transform applied to the sampled values. Default: identity.
         */
        public Image2D setOutTransform(float[] matrix4x4) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTTRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Offset added to the output transform result. Default: (0, 0, 0, 0).
         */
        public Image2D setOutOffset(float[] offset) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTOFFSET, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, offset));
            }
        }
    }

    /**
     * A three dimensional image sampler.
     * <br>
     * Extension KHR_SAMPLER_IMAGE3D
     */
    public static class Image3D extends Sampler<Image3D> {

        public Image3D(Device device, MemorySegment pointer) {
            super(device, SubType.IMAGE3D.name, pointer);
        }

        /**
         * Surface attribute used as texture coordinate. Default: attribute0.
         */
        public Image3D setInAttribute(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_INATTRIBUTE, value);
        }

        /**
         * Transform applied to the input attribute before sampling. Default: identity.
         */
        public Image3D setInTransform(float[] matrix4x4) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_INTRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Offset added to the input transform result. Default: (0, 0, 0, 0).
         */
        public Image3D setInOffset(float[] offset) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_INOFFSET, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, offset));
            }
        }

        /**
         * Array backing the sampler.
         */
        public Image3D setImage(Array3D value) throws Throwable {
            return setObject(Properties.Sampler.PARAM_IMAGE, DataType.ARRAY3D, value);
        }

        /**
         * Filter of the sampler, possible values: nearest, linear. Default: linear.
         */
        public Image3D setFilter(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_FILTER, value);
        }

        /**
         * Wrap mode of the sampler for the 1st dimension. Default: clampToEdge.
         */
        public Image3D setWrapMode1(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_WRAPMODE1, value);
        }

        /**
         * Wrap mode of the sampler for the 2nd dimension. Default: clampToEdge.
         */
        public Image3D setWrapMode2(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_WRAPMODE2, value);
        }

        /**
         * Wrap mode of the sampler for the 3rd dimension. Default: clampToEdge.
         */
        public Image3D setWrapMode3(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_WRAPMODE3, value);
        }

        /**
         * Transform applied to the sampled values. Default: identity.
         */
        public Image3D setOutTransform(float[] matrix4x4) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTTRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Offset added to the output transform result. Default: (0, 0, 0, 0).
         */
        public Image3D setOutOffset(float[] offset) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTOFFSET, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, offset));
            }
        }
    }

    /**
     * Samples an Array1D at integer coordinates based on the primitiveId
     * surface attribute. Serves a similar role as per-primitive attributes on
     * geometries, but is attached to the material instead.
     * <br>
     * Extension KHR_SAMPLER_PRIMITIVE
     */
    public static class Primitive extends Sampler<Primitive> {

        public Primitive(Device device, MemorySegment pointer) {
            super(device, SubType.PRIMITIVE.name, pointer);
        }

        /**
         * Backing array of the sampler.
         */
        public Primitive setArray(Array1D value) throws Throwable {
            return setObject(Properties.Sampler.PARAM_ARRAY, DataType.ARRAY1D, value);
        }

        /**
         * Offset added to primitiveId before sampling. Default: 0.
         */
        public Primitive setInOffset(long offset) throws Throwable {
            return setUInt64(Properties.Sampler.PARAM_INOFFSET, offset);
        }

    }

    /**
     * Uses the input attribute as the "sampled" value, without actually
     * sampling any array or image; useful for swizzling or scaling components.
     * <br>
     * Extension KHR_SAMPLER_TRANSFORM
     */
    public static class Transform extends Sampler<Transform> {

        public Transform(Device device, MemorySegment pointer) {
            super(device, SubType.TRANSFORM.name, pointer);
        }

        /**
         * Surface attribute used as input. Default: attribute0.
         */
        public Transform setInAttribute(String value) throws Throwable {
            return setString(Properties.Sampler.PARAM_INATTRIBUTE, value);
        }

        /**
         * Transform applied to the sampled values. Default: identity.
         */
        public Transform setOutTransform(float[] matrix4x4) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTTRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Offset added to the output transform result. Default: (0, 0, 0, 0).
         */
        public Transform setOutOffset(float[] offset) throws Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Sampler.PARAM_OUTOFFSET, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, offset));
            }
        }
    }
}
