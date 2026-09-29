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
import java.lang.foreign.ValueLayout;
import java.util.function.BiFunction;

/**
 * Volumes in ANARI represent volumetric objects (complementing surfaces),
 * encapsulating spatial data as well as appearance information.
 *
 * {@snippet lang=c :
 * typedef void* ANARIVolume;
 * }
 *
 * @author Johann Sorel
 */
public class Volume<T extends Volume<T>> extends Object<T> {

    public static class SubType<T extends Volume> extends Object.SubType<T>{

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<TransferFunction1D> TRANSFERFUNCTION1D = new SubType<>("transferFunction1D",TransferFunction1D::new);
    }

    public Volume(Device device, String subtype, MemorySegment pointer) {
        super(device, DataType.VOLUME, subtype, pointer);
    }

    /**
     * Whether the volume is visible. Default: TRUE.
     */
    public T setVisible(boolean visible) throws AnariException, Throwable {
        return setBool(Properties.Volume.PARAM_VISIBLE, visible);
    }

    /**
     * The 1D transfer function volume represents (monochromatic) absorbing and
     * (colored) light emitting particles whose local density is defined at each
     * point of the spatial field value by the final opacity and unitDistance.
     * <br>
     * Extension KHR_VOLUME_TRANSFER_FUNCTION1D
     */
    public static class TransferFunction1D extends Volume<TransferFunction1D> {

        public TransferFunction1D(Device device, MemorySegment pointer) {
            super(device, SubType.TRANSFERFUNCTION1D.name, pointer);
        }

        /**
         * Spatial field used for the scalar values of the volume.
         */
        public TransferFunction1D setValue(SpatialField value) throws Throwable {
            return setObject(Properties.Volume.PARAM_VALUE, DataType.SPATIAL_FIELD, value);
        }

        /**
         * Sampled values of value are clamped to this range. Default: [0, 1].
         */
        public TransferFunction1D setValueRange(float start, float end) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Volume.PARAM_VALUERANGE, DataType.FLOAT32_BOX1, tempArena.allocateFrom(ValueLayout.JAVA_FLOAT, start, end));
            }
        }

        /**
         * Sampled values of value are clamped to this range. Default: [0, 1].
         */
        public TransferFunction1D setValueRange(double start, double end) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Volume.PARAM_VALUERANGE, DataType.FLOAT64_BOX1, tempArena.allocateFrom(ValueLayout.JAVA_DOUBLE, start, end));
            }
        }

        /**
         * Uniform color to which the sampled values are mapped. Default: (1, 1, 1, 1).
         */
        public TransferFunction1D setColor(float r, float g, float b) throws AnariException, Throwable {
            return setFloat32Vec3(Properties.Volume.PARAM_COLOR, r,g,b);
        }

        /**
         * Uniform color (with alpha) to which the sampled values are mapped.
         * The fourth component is multiplied with the resulting opacity to form
         * the final opacity. Default: (1, 1, 1, 1).
         */
        public TransferFunction1D setColor(float r, float g, float b, float a) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Volume.PARAM_COLOR, DataType.FLOAT32_VEC4, tempArena.allocateFrom(ValueLayout.JAVA_FLOAT, r,g,b,a));
            }
        }

        /**
         * Color look-up table: the first array element represents the lowest
         * value in valueRange and the last element the highest, linearly
         * interpolated in between. Can have a different size than the opacity array.
         */
        public TransferFunction1D setColor(Array1D color) throws AnariException, Throwable {
            return setObject(Properties.Volume.PARAM_COLOR, DataType.ARRAY1D, color);
        }

        /**
         * Uniform opacity to which the sampled values are mapped. Default: 1.0.
         */
        public TransferFunction1D setOpacity(float opacity) throws AnariException, Throwable {
            return setFloat32(Properties.Volume.PARAM_OPACITY, opacity);
        }

        /**
         * Opacity look-up table: the first array element represents the lowest
         * value in valueRange and the last element the highest, linearly
         * interpolated in between. Can have a different size than the color array.
         */
        public TransferFunction1D setOpacity(Array1D opacity) throws AnariException, Throwable {
            return setObject(Properties.Volume.PARAM_OPACITY, DataType.ARRAY1D, opacity);
        }

        /**
         * Distance after which an opacity fraction of light traveling through the
         * volume is absorbed. Default: 1.0.
         */
        public TransferFunction1D setUnitDistance(float unitDistance) throws AnariException, Throwable {
            return setFloat32(Properties.Volume.PARAM_UNITDISTANCE, unitDistance);
        }

        /**
         * Optional user id, for frame channel objectId. Default: -1u.
         * <br>
         * Extension KHR_FRAME_CHANNEL_OBJECT_ID
         */
        public TransferFunction1D setId(int id) throws AnariException, Throwable {
            try (Arena tempArena = Arena.ofConfined()){
                return set(Properties.Volume.PARAM_ID, DataType.UINT32, tempArena.allocateFrom(ValueLayout.JAVA_INT, id));
            }
        }
    }
}
