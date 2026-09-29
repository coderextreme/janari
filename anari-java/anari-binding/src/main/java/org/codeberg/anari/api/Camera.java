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
 * Cameras express viewpoint and viewport projection information for rendering
 * a scene. ANARI uses a right-handed coordinate system.
 *
 * {@snippet lang=c :
 * typedef void* ANARICamera;
 * }
 *
 * @author Johann Sorel
 */
public class Camera<T extends Camera<T>> extends Object<T> {


    public static class SubType<T extends Camera> extends Object.SubType<T> {

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<Perspective> PERSPECTIVE = new SubType<>("perspective",Perspective::new);
        public static final SubType<Omnidirectional> OMNIDIRECTIONAL = new SubType<>("omnidirectional",Omnidirectional::new);
        public static final SubType<Orthographic> ORTHOGRAPHIC = new SubType<>("orthographic",Orthographic::new);
    }


    public Camera(Device device, String subtype, MemorySegment pointer) {
        super(device, DataType.CAMERA, subtype, pointer);
    }

    /**
     * Position of the camera in world-space. Default: (0, 0, 0).
     */
    public T setPosition(float x, float y, float z) throws AnariException, Throwable {
        return setFloat32Vec3(Properties.Camera.PARAM_POSITION, x, y, z);
    }

    /**
     * Main viewing direction of the camera. Default: (0, 0, -1).
     */
    public T setDirection(float x, float y, float z) throws AnariException, Throwable {
        return setFloat32Vec3(Properties.Camera.PARAM_DIRECTION, x, y, z);
    }

    /**
     * Up direction of the camera. Default: (0, 1, 0).
     */
    public T setUp(float x, float y, float z) throws AnariException, Throwable {
        return setFloat32Vec3(Properties.Camera.PARAM_UP, x, y, z);
    }

    /**
     * Region of the sensor that is rendered to the image, in normalized
     * screen-space coordinates (minx, miny, maxx, maxy). Can be used to crop
     * the image, achieve asymmetrical view frusta, or flip the image
     * horizontally. Values outside [0, 1] are valid (overscan, film gate, or
     * shifted sensor). Default: ((0, 0), (1, 1)).
     */
    public T setImageRegion(float minx, float miny, float maxx, float maxy) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Camera.PARAM_IMAGEREGION, DataType.FLOAT32_BOX2, tempArena.allocateFrom(JAVA_FLOAT, minx, miny, maxx, maxy));
        }
    }

    /**
     * Size of the aperture, controlling the depth of field. Default: 0.
     * <br>
     * Extension KHR_CAMERA_DEPTH_OF_FIELD
     */
    public T setApertureRadius(float apertureRadius) throws AnariException, Throwable {
        return setFloat32(Properties.Camera.PARAM_APERTURERADIUS, apertureRadius);
    }

    /**
     * Distance at which the image is sharpest when depth of field is enabled. Default: 1.
     * <br>
     * Extension KHR_CAMERA_DEPTH_OF_FIELD
     */
    public T setFocusDistance(float focusDistance) throws AnariException, Throwable {
        return setFloat32(Properties.Camera.PARAM_FOCUSDISTANCE, focusDistance);
    }

    /**
     * Possible values: none, left, right, sideBySide, topBottom (left eye at
     * top half). Default: none.
     * <br>
     * Extension KHR_CAMERA_STEREO
     */
    public T setStereoMode(String stereoMode) throws AnariException, Throwable {
        return setString(Properties.Camera.PARAM_STEREOMODE, stereoMode);
    }

    /**
     * Distance between left and right eye when stereo is enabled. Default: 0.0635.
     * <br>
     * Extension KHR_CAMERA_STEREO
     */
    public T setInterpupillaryDistance(float interpupillaryDistance) throws AnariException, Throwable {
        return setFloat32(Properties.Camera.PARAM_INTERPUPILLARYDISTANCE, interpupillaryDistance);
    }

    /**
     * Uniformly distributed world-space transformations, as an Array1D of
     * FLOAT32_MAT4, to achieve camera motion blur (in combination with
     * extension KHR_CAMERA_SHUTTER).
     * <br>
     * Extension KHR_CAMERA_MOTION_TRANSFORMATION
     */
    public T setMotionTransform(Array1D array) throws AnariException, Throwable {
        return setObject(Properties.Camera.PARAM_MOTION_TRANSFORM, DataType.ARRAY1D, array);
    }

    /**
     * Uniformly distributed scale keys, as an Array1D of FLOAT32_VEC3,
     * overridden by motion.transform.
     * <br>
     * Extension KHR_CAMERA_MOTION_TRANSFORMATION
     */
    public T setMotionScale(Array1D array) throws AnariException, Throwable {
        return setObject(Properties.Camera.PARAM_MOTION_SCALE, DataType.ARRAY1D, array);
    }

    /**
     * Uniformly distributed quaternion rotation keys, as an Array1D of
     * FLOAT32_QUAT_IJKW, overridden by motion.transform.
     * <br>
     * Extension KHR_CAMERA_MOTION_TRANSFORMATION
     */
    public T setMotionRotation(Array1D array) throws AnariException, Throwable {
        return setObject(Properties.Camera.PARAM_MOTION_ROTATION, DataType.ARRAY1D, array);
    }

    /**
     * Uniformly distributed translation keys, as an Array1D of FLOAT32_VEC3,
     * overridden by motion.transform.
     * <br>
     * Extension KHR_CAMERA_MOTION_TRANSFORMATION
     */
    public T setMotionTranslation(Array1D array) throws AnariException, Throwable {
        return setObject(Properties.Camera.PARAM_MOTION_TRANSLATION, DataType.ARRAY1D, array);
    }

    /**
     * Time associated with the first and last key in the motion.* arrays.
     * Default: [0, 1].
     * <br>
     * Extension KHR_CAMERA_MOTION_TRANSFORMATION
     */
    public T setTime(float start, float end) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Camera.PARAM_TIME, DataType.FLOAT32_BOX1, tempArena.allocateFrom(JAVA_FLOAT, start, end));
        }
    }

    /**
     * Start and end of the shutter time, clamped to [0, 1]. Default: [0.5, 0.5].
     * <br>
     * Extension KHR_CAMERA_SHUTTER
     */
    public T setShutter(float start, float end) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Camera.PARAM_SHUTTER, DataType.FLOAT32_BOX1, tempArena.allocateFrom(JAVA_FLOAT, start, end));
        }
    }

    /**
     * Rolling direction of the shutter, possible values: none, left, right,
     * down, up. Default: none.
     * <br>
     * Extension KHR_CAMERA_ROLLING_SHUTTER
     */
    public T setRollingShutterDirection(String rollingShutterDirection) throws AnariException, Throwable {
        return setString(Properties.Camera.PARAM_ROLLINGSHUTTERDIRECTION, rollingShutterDirection);
    }

    /**
     * The "open" time per line, clamped to [0, shutter.upper - shutter.lower]. Default: 0.
     * <br>
     * Extension KHR_CAMERA_ROLLING_SHUTTER
     */
    public T setRollingShutterDuration(float rollingShutterDuration) throws AnariException, Throwable {
        return setFloat32(Properties.Camera.PARAM_ROLLINGSHUTTERDURATION, rollingShutterDuration);
    }

    /**
     * A simple thin lens camera for perspective rendering.
     * <br>
     * Extension KHR_CAMERA_PERSPECTIVE
     */
    public static class Perspective extends Camera<Perspective> {

        public Perspective(Device device, MemorySegment pointer) {
            super(device, SubType.PERSPECTIVE.name, pointer);
        }

        /**
         * The field of view (angle in radians) of the frame's height. Default: π/3.
         */
        public Perspective setFovY(float fovy) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_FOVY, fovy);
        }

        /**
         * Ratio of width by height of the frame (and image region). Default: 1.
         */
        public Perspective setAspect(float aspect) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_ASPECT, aspect);
        }

        /**
         * Near clip plane distance. Must satisfy 0 &lt; near &lt; far when set;
         * otherwise determined by the renderer.
         */
        public Perspective setNear(float near) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_NEAR, near);
        }

        /**
         * Far clip plane distance. Must satisfy 0 &lt; near &lt; far when set;
         * otherwise determined by the renderer.
         */
        public Perspective setFar(float far) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_FAR, far);
        }
    }

    /**
     * A camera capturing the complete surrounding.
     * <br>
     * Extension KHR_CAMERA_OMNIDIRECTIONAL
     */
    public static class Omnidirectional extends Camera<Omnidirectional> {

        public Omnidirectional(Device device, MemorySegment pointer) {
            super(device, SubType.OMNIDIRECTIONAL.name, pointer);
        }

        /**
         * Pixel layout, possible values: equirectangular. Default: equirectangular.
         */
        public Omnidirectional setLayout(String layout) throws AnariException, Throwable {
            return setString(Properties.Camera.PARAM_LAYOUT, layout);
        }
    }

    /**
     * A simple camera with orthographic projection.
     * <br>
     * Extension KHR_CAMERA_ORTHOGRAPHIC
     */
    public static class Orthographic extends Camera<Orthographic> {

        public Orthographic(Device device, MemorySegment pointer) {
            super(device, SubType.ORTHOGRAPHIC.name, pointer);
        }

        /**
         * Ratio of width by height of the frame (and image region). Default: 1.
         */
        public Orthographic setAspect(float aspect) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_ASPECT, aspect);
        }

        /**
         * Height of the image plane in world units. Default: 1.
         */
        public Orthographic setHeight(float height) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_HEIGHT, height);
        }

        /**
         * Near clip plane distance. Must satisfy 0 &le; near &lt; far when set;
         * otherwise determined by the renderer.
         */
        public Orthographic setNear(float near) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_NEAR, near);
        }

        /**
         * Far clip plane distance. Must satisfy 0 &le; near &lt; far when set;
         * otherwise determined by the renderer.
         */
        public Orthographic setFar(float far) throws AnariException, Throwable {
            return setFloat32(Properties.Camera.PARAM_FAR, far);
        }
    }
}
