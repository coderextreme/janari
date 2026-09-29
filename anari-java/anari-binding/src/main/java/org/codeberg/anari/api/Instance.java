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
import static java.lang.foreign.ValueLayout.JAVA_INT;
import java.util.function.BiFunction;

/**
 * Instances apply transforms to groups for placement in the World. All
 * instances take a Group representing all the objects which share the
 * instance's world-space transform.
 *
 * {@snippet lang=c :
 * typedef void* ANARIInstance;
 * }
 *
 * @author Johann Sorel
 */
public class Instance<T extends Instance<T>> extends Object<T> {

    public static class SubType<T extends Instance> extends Object.SubType<T>{

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<Transform> TRANSFORM = new SubType<>("transform",Transform::new);
        public static final SubType<MotionTransform> MOTION_TRANSFORM = new SubType<>("motionTransform",MotionTransform::new);
        public static final SubType<MotionScaleRotationTranslation> MOTION_SCALE_ROTATION_TRANSLATION = new SubType<>("motionScaleRotationTranslation",MotionScaleRotationTranslation::new);
    }

    public Instance(Device device, String subtype, MemorySegment pointer) {
        super(device, DataType.INSTANCE, subtype, pointer);
    }

    /**
     * Required Group to be instanced.
     */
    public T setGroup(Group group) throws AnariException, Throwable {
        return setObject(Properties.Instance.PARAM_GROUP, DataType.GROUP, group);
    }

    /**
     * World-space transformation matrix (column-major) for all attached objects.
     * Default: identity. Since the parameters of all other instance subtypes are
     * supersets of this one, this should always be set in addition to any
     * subtype specific transform parameters.
     */
    public T setTransform(float[] matrix4x4) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Instance.PARAM_TRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
        }
    }

    /**
     * Optional user id, for frame channel instanceId. Default: -1u.
     * <br>
     * Extension KHR_FRAME_CHANNEL_INSTANCE_ID
     */
    public T setId(int id) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Instance.PARAM_ID, DataType.UINT32, tempArena.allocateFrom(JAVA_INT, id));
        }
    }

    /**
     * Uniform color attribute, applied to all objects contained in the instance.
     * Has a lower precedence than attribute values found in objects within the instance.
     */
    public T setColor(float r, float g, float b, float a) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Instance.PARAM_COLOR, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, r, g, b, a));
        }
    }

    /**
     * Uniform attribute 0, applied to all objects contained in the instance.
     * Has a lower precedence than attribute values found in objects within the instance.
     */
    public T setAttribute0(float x, float y, float z, float w) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Instance.PARAM_ATTRIBUTE0, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Uniform attribute 1, applied to all objects contained in the instance.
     * Has a lower precedence than attribute values found in objects within the instance.
     */
    public T setAttribute1(float x, float y, float z, float w) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Instance.PARAM_ATTRIBUTE1, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Uniform attribute 2, applied to all objects contained in the instance.
     * Has a lower precedence than attribute values found in objects within the instance.
     */
    public T setAttribute2(float x, float y, float z, float w) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Instance.PARAM_ATTRIBUTE2, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Uniform attribute 3, applied to all objects contained in the instance.
     * Has a lower precedence than attribute values found in objects within the instance.
     */
    public T setAttribute3(float x, float y, float z, float w) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Instance.PARAM_ATTRIBUTE3, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, x, y, z, w));
        }
    }

    /**
     * Axis-aligned bounding box in world-space (excluding the lights), queried as a property.
     */
    public float[] getBounds() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment worldBounds = tempArena.allocate(6*4);
            final int res = getProperty(Properties.Instance.QUERY_BOUNDS, DataType.FLOAT32_BOX3, worldBounds, 6*4, WaitMask.WAIT);
            if (res == 0) return null;
            final float[] wbf = new float[6];
            worldBounds.asByteBuffer().asFloatBuffer().get(wbf);
            return wbf;
        }
    }

    /**
     * A basic instance implementing only the parameters and properties common to
     * all instances.
     * <br>
     * Extension KHR_INSTANCE_TRANSFORM
     */
    public static class Transform extends Instance<Transform> {

        public Transform(Device device, MemorySegment pointer) {
            super(device, SubType.TRANSFORM.name, pointer);
        }

        /**
         * Array of matrices, instantiating the group once per element, giving an
         * efficient means to transform a single group many times without creating
         * one instance object per transform.
         * <br>
         * Extension KHR_INSTANCE_TRANSFORM_ARRAY
         */
        public Transform setTransform(Array1D matrices) throws AnariException, Throwable {
            return setObject(Properties.Instance.PARAM_TRANSFORM, DataType.ARRAY1D, matrices);
        }

        /**
         * Optional per-element user id array, applied to each element instance of
         * the transform array, for frame channel instanceId. Must be at least as
         * large as the transform array; if absent, the single-value id is used.
         * <br>
         * Extension KHR_INSTANCE_TRANSFORM_ARRAY, KHR_FRAME_CHANNEL_INSTANCE_ID
         */
        public Transform setId(Array1D ids) throws AnariException, Throwable {
            return setObject(Properties.Instance.PARAM_ID, DataType.ARRAY1D, ids);
        }
    }

    /**
     * Represents uniformly (in time) distributed transformation keys to achieve
     * transformation motion blur, in combination with time and extension
     * KHR_CAMERA_SHUTTER. The motion.transform parameter takes precedence over
     * the generic transform parameter.
     * <br>
     * Extension KHR_INSTANCE_MOTION_TRANSFORM
     */
    public static class MotionTransform extends Instance<MotionTransform> {

        public MotionTransform(Device device, MemorySegment pointer) {
            super(device, SubType.MOTION_TRANSFORM.name, pointer);
        }

        /**
         * Uniformly distributed world-space transformations, as an Array1D of matrices.
         */
        public MotionTransform setMotionTransform(Array1D array) throws AnariException, Throwable {
            return setObject(Properties.Instance.PARAM_MOTION_TRANSFORM, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed world-space transformations, as a single matrix.
         */
        public MotionTransform setMotionTransform(float[] matrix4x4) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Instance.PARAM_MOTION_TRANSFORM, DataType.FLOAT32_MAT4, tempArena.allocateFrom(JAVA_FLOAT, matrix4x4));
            }
        }

        /**
         * Time (start, end) associated with the first and last key in the
         * motion.transform array. Default: [0, 1].
         */
        public MotionTransform setTime(float[] box) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Instance.PARAM_TIME, DataType.FLOAT32_BOX1, tempArena.allocateFrom(JAVA_FLOAT, box));
            }
        }

    }

    /**
     * Represents uniformly (in time) distributed transformation keys as defined
     * by decomposed scale, rotation and translation components (applied in this
     * order) to achieve transformation motion blur, in combination with time and
     * extension KHR_CAMERA_SHUTTER. The motion.* parameters take precedence over
     * the generic transform parameter.
     * <br>
     * Extension KHR_INSTANCE_MOTION_SCALE_ROTATION_TRANSLATION
     */
    public static class MotionScaleRotationTranslation extends Instance<MotionScaleRotationTranslation> {

        public MotionScaleRotationTranslation(Device device, MemorySegment pointer) {
            super(device, SubType.MOTION_SCALE_ROTATION_TRANSLATION.name, pointer);
        }

        /**
         * Uniformly distributed scale keys, as an Array1D of FLOAT32_VEC3.
         */
        public MotionScaleRotationTranslation setMotionScale(Array1D array) throws AnariException, Throwable {
            return setObject(Properties.Instance.PARAM_MOTION_SCALE, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed scale, as a single FLOAT32_VEC3.
         */
        public MotionScaleRotationTranslation setMotionScale(float[] scale) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Instance.PARAM_MOTION_SCALE, scale);
        }

        /**
         * Uniformly distributed quaternion rotation keys, as an Array1D of FLOAT32_QUAT_IJKW.
         */
        public MotionScaleRotationTranslation setMotionRotation(Array1D array) throws AnariException, Throwable {
            return setObject(Properties.Instance.PARAM_MOTION_ROTATION, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed quaternion rotation, as a single FLOAT32_QUAT_IJKW.
         */
        public MotionScaleRotationTranslation setMotionRotation(float[] quaternion) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Instance.PARAM_MOTION_ROTATION, DataType.FLOAT32_QUAT_IJKW, tempArena.allocateFrom(JAVA_FLOAT, quaternion));
            }
        }

        /**
         * Uniformly distributed translation keys, as an Array1D of FLOAT32_VEC3.
         */
        public MotionScaleRotationTranslation setMotionTranslation(Array1D array) throws AnariException, Throwable {
            return setObject(Properties.Instance.PARAM_MOTION_TRANSLATION, DataType.ARRAY1D, array);
        }

        /**
         * Uniformly distributed translation, as a single FLOAT32_VEC3.
         */
        public MotionScaleRotationTranslation setMotionTranslation(float[] translation) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Instance.PARAM_MOTION_TRANSLATION, translation);
        }

        /**
         * Time (start, end) associated with the first and last key in the
         * motion.* arrays. Default: [0, 1].
         */
        public MotionScaleRotationTranslation setTime(float[] box) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Instance.PARAM_TIME, DataType.FLOAT32_BOX1, tempArena.allocateFrom(JAVA_FLOAT, box));
            }
        }
    }
}
