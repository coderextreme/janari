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

import java.lang.foreign.MemorySegment;
import java.util.function.BiFunction;

/**
 * Lights in ANARI are virtual objects that emit light into the world and thus
 * illuminate objects. The transformation of an instance applies only to
 * vectors like position or direction of a light, not to sizes like radius.
 *
 * {@snippet lang=c :
 * typedef void* ANARILight;
 * }
 *
 * @author Johann Sorel
 */
public class Light<T extends Light<T>> extends Object<T> {

    public static class SubType<T extends Light> extends Object.SubType<T>{

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<Directional> DIRECTIONAL = new SubType<>("directional",Directional::new);
        public static final SubType<HDRI> HDRI = new SubType<>("hdri",HDRI::new);
        public static final SubType<Point> POINT = new SubType<>("point",Point::new);
        public static final SubType<Quad> QUAD = new SubType<>("quad",Quad::new);
        public static final SubType<Ring> RING = new SubType<>("ring",Ring::new);
        public static final SubType<Spot> SPOT = new SubType<>("spot",Spot::new);
    }

    /**
     * Color of the light, in 0..1. This is a unitless factor and acts as a
     * filter of the emitted light. Default: (1, 1, 1).
     */
    public T setColor(float r, float g, float b) throws AnariException, Throwable {
        return setFloat32Vec3(Properties.Light.PARAM_COLOR, r,g,b);
    }

    /**
     * Whether the light can be directly seen (only meaningful for area
     * lights). Default: TRUE.
     * <br>
     * Extension KHR_LIGHT_PRIMARY_VISIBILITY
     */
    public T setVisible(boolean visible) throws AnariException, Throwable {
        return setBool(Properties.Light.PARAM_VISIBLE, visible);
    }

    public Light(Device device, String subType, MemorySegment pointer) {
        super(device, DataType.LIGHT, subType, pointer);
    }

    /**
     * A light thought to be far away (outside of the scene), so its light
     * arrives (mostly) as parallel rays.
     * <br>
     * Extension KHR_LIGHT_DIRECTIONAL
     */
    public static class Directional extends Light<Directional> {

        public Directional(Device device, MemorySegment pointer) {
            super(device, SubType.DIRECTIONAL.name, pointer);
        }

        /**
         * Main emission direction of the directional light. Default: (0, 0, -1).
         */
        public Directional setDirection(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_DIRECTION, x,y,z);
        }

        /**
         * Amount of light arriving at a surface point, assuming the light is
         * oriented towards the surface, in W/m2. Default: 1. Takes precedence
         * over radiance if both are explicitly set.
         */
        public Directional setIrradiance(float irradiance) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_IRRADIANCE, irradiance);
        }

        /**
         * Apparent size (angle in radians) of the light. Default: 0. A good
         * approximation for sunlight is about 0.00925 rad.
         */
        public Directional setAngularDiameter(float angularDiameter) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_ANGULARDIAMETER, angularDiameter);
        }

        /**
         * Alternative specification of the brightness (if irradiance is not
         * explicitly set): the amount of light emitted in a direction, in
         * W/sr/m2. Only meaningful if angularDiameter is larger than zero.
         */
        public Directional setRadiance(float radiance) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_RADIANCE, radiance);
        }
    }

    /**
     * A textured light source surrounding the scene and illuminating it from
     * infinity.
     * <br>
     * Extension KHR_LIGHT_HDRI
     */
    public static class HDRI extends Light<HDRI> {

        public HDRI(Device device, MemorySegment pointer) {
            super(device, SubType.HDRI.name, pointer);
        }

        /**
         * Up direction of the light in world-space. Default: (0, 0, 1).
         */
        public HDRI setUp(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_UP, x,y,z);
        }

        /**
         * Direction to which the center of the texture will be mapped. Default: (1, 0, 0).
         */
        public HDRI setDirection(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_DIRECTION, x,y,z);
        }

        /**
         * Uniform environment radiance, typically HDR with values &gt; 1, the
         * amount of light emitted by a point on the light source in a
         * direction, in W/sr/m2.
         */
        public HDRI setRadiance(float r0, float r1, float r2) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_RADIANCE, r0,r1,r2);
        }

        /**
         * Environment map, typically HDR with values &gt; 1, the amount of
         * light emitted by a point on the light source in a direction, in W/sr/m2.
         */
        public HDRI setRadiance(Array2D radiance) throws AnariException, Throwable {
            return setObject(Properties.Light.PARAM_RADIANCE, DataType.ARRAY2D, radiance);
        }

        /**
         * Possible values: equirectangular. Default: equirectangular.
         */
        public HDRI setLayout(String layout) throws AnariException, Throwable {
            return setString(Properties.Light.PARAM_LAYOUT, layout);
        }

        /**
         * Scale factor for radiance. Default: 1.
         */
        public HDRI setScale(float radiance) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_SCALE, radiance);
        }
    }

    /**
     * A light emitting uniformly in all directions from the surface toward
     * the outside (or, with radius &gt; 0, a sphere light).
     * <br>
     * Extension KHR_LIGHT_POINT
     */
    public static class Point extends Light<Point> {

        public Point(Device device, MemorySegment pointer) {
            super(device, SubType.POINT.name, pointer);
        }

        /**
         * The position of the point light. Default: (0, 0, 0).
         */
        public Point setPosition(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_POSITION, x,y,z);
        }

        /**
         * The overall amount of light emitted by the light in a direction, in
         * W/sr. Default: 1. Takes precedence over power and radiance if
         * explicitly set (in that order).
         */
        public Point setIntensity(float intensity) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_INTENSITY, intensity);
        }

        /**
         * Alternative specification of the brightness (if intensity is not
         * explicitly set): the overall amount of light energy emitted, in W.
         */
        public Point setPower(float power) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_POWER, power);
        }

        /**
         * The size of the point light, turning it into a sphere light. Default: 0.
         */
        public Point setRadius(float radius) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_RADIUS, radius);
        }

        /**
         * Alternative specification of the brightness (if neither intensity
         * nor power is explicitly set): the amount of light emitted by a
         * point on the light source in a direction, in W/sr/m2. Only
         * meaningful if radius is larger than zero.
         */
        public Point setRadiance(float radiance) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_RADIANCE, radiance);
        }
    }

    /**
     * A planar, procedural area light source (parallelogram) emitting
     * uniformly either into one half-space or both.
     * <br>
     * Extension KHR_LIGHT_QUAD
     */
    public static class Quad extends Light<Quad> {

        public Quad(Device device, MemorySegment pointer) {
            super(device, SubType.QUAD.name, pointer);
        }

        /**
         * Position of one vertex of the quad light. Default: (0, 0, 0).
         */
        public Quad setPosition(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_POSITION, x,y,z);
        }

        /**
         * Vector to one adjacent vertex. Default: (1, 0, 0).
         */
        public Quad setEdge1(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_EDGE1, x,y,z);
        }

        /**
         * Vector to the other adjacent vertex. Default: (0, 1, 0). The front
         * side is determined by the cross product of edge2 x edge1.
         */
        public Quad setEdge2(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_EDGE2, x,y,z);
        }

        /**
         * The overall amount of light emitted by the light in a direction, in
         * W/sr. Default: 1. Takes precedence over power and radiance if
         * explicitly set (in that order).
         */
        public Quad setIntensity(float intensity) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_INTENSITY, intensity);
        }

        /**
         * Alternative specification of the brightness (if intensity is not
         * explicitly set): the overall amount of light energy emitted, in W.
         */
        public Quad setPower(float power) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_POWER, power);
        }

        /**
         * Alternative specification of the brightness (if neither intensity
         * nor power is explicitly set): the amount of light emitted by a
         * point on the light source in a direction, in W/sr/m2.
         */
        public Quad setRadiance(float radiance) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_RADIANCE, radiance);
        }

        /**
         * Side into which light is emitted, possible values: front, back,
         * both. Default: front.
         */
        public Quad setSide(String side) throws AnariException, Throwable {
            return setString(Properties.Light.PARAM_SIDE, side);
        }

        /**
         * Luminous intensity distribution for photometric lights (1D, uniform
         * around direction). Values are uniformly mapped to gamma in [0, pi].
         */
        public Quad setIntensityDistribution(Array1D intensityDistribution) throws AnariException, Throwable {
            return setObject(Properties.Light.PARAM_INTENSITYDISTRIBUTION, DataType.ARRAY1D, intensityDistribution);
        }

        /**
         * Luminous intensity distribution for photometric lights (2D, for
         * asymmetric illumination). The orientation of the C0-plane is
         * aligned with edge1.
         */
        public Quad setIntensityDistribution(Array2D intensityDistribution) throws AnariException, Throwable {
            return setObject(Properties.Light.PARAM_INTENSITYDISTRIBUTION, DataType.ARRAY2D, intensityDistribution);
        }

        /**
         * Luminous intensity distribution for photometric lights, single scalar value.
         */
        public Quad setIntensityDistribution(float intensityDistribution) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_INTENSITYDISTRIBUTION, intensityDistribution);
        }

    }

    /**
     * A light emitting into a cone of directions, with a cosine falloff (an area light).
     * <br>
     * Extension KHR_LIGHT_RING
     */
    public static class Ring extends Light<Ring> {

        public Ring(Device device, MemorySegment pointer) {
            super(device, SubType.RING.name, pointer);
        }

        /**
         * The center of the ring light. Default: (0, 0, 0).
         */
        public Ring setPosition(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_POSITION, x,y,z);
        }

        /**
         * Main emission direction, the center axis of the ring. Default: (0, 0, -1).
         */
        public Ring setDirection(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_DIRECTION, x,y,z);
        }

        /**
         * Full opening angle (in radians) of the cone of directions; outside
         * of this cone there is no illumination. Default: pi.
         */
        public Ring setOpeningAngle(float openingAngle) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_OPENINGANGLE, openingAngle);
        }

        /**
         * Size (angle in radians) of the region between the rim of the
         * illumination cone and full intensity; should be smaller than half
         * of openingAngle. Default: 0.1.
         */
        public Ring setFalloffAngle(float falloffAngle) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_FALLOFFANGLE, falloffAngle);
        }

        /**
         * The overall amount of light emitted by the light in a direction, in
         * W/sr. Default: 1. Takes precedence over power and radiance if
         * explicitly set (in that order).
         */
        public Ring setIntensity(float intensity) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_INTENSITY, intensity);
        }

        /**
         * Alternative specification of the brightness (if intensity is not
         * explicitly set): the overall amount of light energy emitted, in W.
         */
        public Ring setPower(float power) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_POWER, power);
        }

        /**
         * The (outer) size of the ring, the radius of a disk with normal
         * direction. Default: 0.
         */
        public Ring setRadius(float radius) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_RADIUS, radius);
        }

        /**
         * In combination with radius, turns the disk into a ring; must be
         * smaller than radius. Default: 0.
         */
        public Ring setInnerRadius(float innerradius) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_INNERRADIUS, innerradius);
        }

        /**
         * Alternative specification of the brightness (if neither intensity
         * nor power is explicitly set): the amount of light emitted by a
         * point on the light source in a direction, in W/sr/m2. Only
         * meaningful if radius is larger than zero.
         */
        public Ring setRadiance(float radiance) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_RADIANCE, radiance);
        }

        /**
         * Luminous intensity distribution for photometric lights (1D, uniform
         * around direction).
         */
        public Ring setIntensityDistribution(Array1D intensityDistribution) throws AnariException, Throwable {
            return setObject(Properties.Light.PARAM_INTENSITYDISTRIBUTION, DataType.ARRAY1D, intensityDistribution);
        }

        /**
         * Luminous intensity distribution for photometric lights (2D, for
         * asymmetric illumination); the orientation of the C0-plane is given by c0.
         */
        public Ring setIntensityDistribution(Array2D intensityDistribution) throws AnariException, Throwable {
            return setObject(Properties.Light.PARAM_INTENSITYDISTRIBUTION, DataType.ARRAY2D, intensityDistribution);
        }

        /**
         * Luminous intensity distribution for photometric lights, single scalar value.
         */
        public Ring setIntensityDistribution(float intensityDistribution) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_INTENSITYDISTRIBUTION, intensityDistribution);
        }

        /**
         * Orientation, i.e. direction of the C0-(half)plane (only needed if
         * illumination via intensityDistribution is asymmetric). Default: (1, 0, 0).
         */
        public Ring setC0(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_C0, x,y,z);
        }
    }

    /**
     * A light emitting into a cone of directions.
     * <br>
     * Extension KHR_LIGHT_SPOT
     */
    public static class Spot extends Light<Spot> {

        public Spot(Device device, MemorySegment pointer) {
            super(device, SubType.SPOT.name, pointer);
        }

        /**
         * The center of the spotlight. Default: (0, 0, 0).
         */
        public Spot setPosition(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_POSITION, x,y,z);
        }

        /**
         * Main emission direction, the axis of the spot. Default: (0, 0, -1).
         */
        public Spot setDirection(float x, float y, float z) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Light.PARAM_DIRECTION, x,y,z);
        }

        /**
         * Full opening angle (in radians) of the spot; outside of this cone
         * there is no illumination. Default: pi.
         */
        public Spot setOpeningAngle(float openingAngle) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_OPENINGANGLE, openingAngle);
        }

        /**
         * Size (angle in radians) of the region between the rim of the
         * illumination cone and full intensity of the spot; should be smaller
         * than half of openingAngle. Default: 0.1.
         */
        public Spot setFalloffAngle(float falloffAngle) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_FALLOFFANGLE, falloffAngle);
        }

        /**
         * The overall amount of light emitted by the light in a direction, in
         * W/sr. Default: 1. Takes precedence over power if both are
         * explicitly set.
         */
        public Spot setIntensity(float intensity) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_INTENSITY, intensity);
        }

        /**
         * Alternative specification of the brightness (if intensity is not
         * explicitly set): the overall amount of light energy emitted, in W.
         */
        public Spot setPower(float power) throws AnariException, Throwable {
            return setFloat32(Properties.Light.PARAM_POWER, power);
        }
    }
}
