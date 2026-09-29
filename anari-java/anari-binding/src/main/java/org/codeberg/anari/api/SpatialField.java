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
 * ANARI spatial field objects define collections of data values spread throughout
 * a local coordinate system in order to be sampled in space.
 *
 * {@snippet lang=c :
 * typedef void* ANARISpatialField;
 * }
 *
 * @author Johann Sorel
 */
public class SpatialField<T extends SpatialField<T>> extends Object<T> {

    public static class SubType<T extends SpatialField> extends Object.SubType<T>{

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            super(name, create);
        }

        public static final SubType<StructuredRegular> STRUCTUREDREGULAR = new SubType<>("structuredRegular",StructuredRegular::new);
        public static final SubType<NanoVDB> NANOVDB = new SubType<>("nanovdb",NanoVDB::new);
        public static final SubType<Unstructured> UNSTRUCTURED = new SubType<>("unstructured",Unstructured::new);
    }

    public SpatialField(Device device, String subtype, MemorySegment pointer) {
        super(device, DataType.SPATIAL_FIELD, subtype, pointer);
    }

    /**
     * A spatial field storing field values on a regular 3D grid, vertex-centered
     * (i.e. the scalars are stored at the grid vertices, not the cells).
     * <br>
     * Extension KHR_SPATIAL_FIELD_STRUCTURED_REGULAR
     */
    public static class StructuredRegular extends SpatialField<StructuredRegular> {

        public StructuredRegular(Device device, MemorySegment pointer) {
            super(device, SubType.STRUCTUREDREGULAR.name, pointer);
        }

        /**
         * The field values for the 3D grid; the size of the spatial field is
         * inferred from the array's dimensions.
         */
        public StructuredRegular setData(Array3D date) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_DATA, DataType.ARRAY3D, date);
        }

        /**
         * Origin of the grid in object-space. Default: (0, 0, 0).
         */
        public StructuredRegular setOrigin(float x, float y, float z) throws Throwable {
            return setFloat32Vec3(Properties.SpatialField.PARAM_ORIGIN, x, y, z);
        }

        /**
         * Size of the grid cells in object-space. Default: (1, 1, 1).
         */
        public StructuredRegular setSpacing(float x, float y, float z) throws Throwable {
            return setFloat32Vec3(Properties.SpatialField.PARAM_SPACING, x, y, z);
        }

        /**
         * Filter used for reconstructing the field, possible values: nearest,
         * linear, cubic (extension KHR_SPATIAL_FIELD_STRUCTURED_REGULAR_FILTER_CUBIC).
         * Default: linear.
         */
        public StructuredRegular setFilter(String filter) throws Throwable {
            return setString(Properties.SpatialField.PARAM_FILTER, filter);
        }
    }

    /**
     * A spatial field backed by a NanoVDB grid, as a binary blob read from a
     * NanoVDB file or constructed by the NanoVDB API. The spatial field extent is
     * implicitly defined in the grid header. Devices must support NanoVDB grids
     * with major version 32.
     * <br>
     * Extension KHR_SPATIAL_FIELD_NANOVDB
     */
    public static class NanoVDB extends SpatialField<NanoVDB> {

        public NanoVDB(Device device, MemorySegment pointer) {
            super(device, SubType.NANOVDB.name, pointer);
        }

        /**
         * The NanoVDB grid, as a binary blob (ARRAY1D of UINT8).
         */
        public NanoVDB setData(Array1D data) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_DATA, DataType.ARRAY1D, data);
        }

        /**
         * Filter used for reconstructing the field, possible values: nearest,
         * linear, cubic. Default: linear.
         */
        public NanoVDB setFilter(String filter) throws Throwable {
            return setString(Properties.SpatialField.PARAM_FILTER, filter);
        }
    }

    /**
     * A spatial field made of unstructured cells (tetrahedral, hexahedral, wedge,
     * or pyramidal), modeled after and compatible with vtkUnstructuredGrid.
     * <br>
     * Extension KHR_SPATIAL_FIELD_UNSTRUCTURED
     */
    public static class Unstructured extends SpatialField<Unstructured> {

        public Unstructured(Device device, MemorySegment pointer) {
            super(device, SubType.UNSTRUCTURED.name, pointer);
        }

        /**
         * Array of vertex positions.
         */
        public Unstructured setVertexPosition(Array1D array) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_VERTEX_POSITION, DataType.ARRAY1D, array);
        }

        /**
         * Array of values at vertices. If both this and cell.data are set,
         * vertex.data takes precedence.
         */
        public Unstructured setVertexData(Array1D array) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_VERTEX_DATA, DataType.ARRAY1D, array);
        }

        /**
         * Array of indices into the vertex.* arrays that form cells.
         */
        public Unstructured setIndex(Array1D array) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_INDEX, DataType.ARRAY1D, array);
        }

        /**
         * Alternative specification of the values (if vertex.data is not
         * explicitly set): array of values in cells.
         */
        public Unstructured setCellData(Array1D array) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_CELL_DATA, DataType.ARRAY1D, array);
        }

        /**
         * Array of cell types, where 10 encodes tetrahedral, 12 hexahedral, 13
         * wedge, and 14 pyramidal cells (matching VTK cell type values).
         */
        public Unstructured setCellType(Array1D array) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_CELL_TYPE, DataType.ARRAY1D, array);
        }

        /**
         * Array of indices into the index array, specifying the first (index of
         * the) vertex of each cell.
         */
        public Unstructured setCellIndex(Array1D array) throws Throwable {
            return setObject(Properties.SpatialField.PARAM_CELL_INDEX, DataType.ARRAY1D, array);
        }
    }
}
