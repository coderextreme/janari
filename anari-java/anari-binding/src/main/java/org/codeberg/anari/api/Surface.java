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
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Geometries are matched with appearance information through Surfaces. These
 * take a Geometry, which defines the spatial representation, and apply either
 * full-object or per-primitive color and material information.
 *
 * {@snippet lang=c :
 * typedef void* ANARISurface;
 * }
 *
 * @author Johann Sorel
 */
public final class Surface extends Object<Surface> {

    public Surface(Device device, MemorySegment pointer) {
        super(device, DataType.SURFACE, null, pointer);
    }

    /**
     * Required Geometry object used by this surface.
     */
    public Surface setGeometry(Geometry geometry) throws Throwable {
        return setObject(Properties.Surface.PARAM_GEOMETRY, DataType.GEOMETRY, geometry);
    }

    /**
     * Required Material applied to the geometry.
     */
    public Surface setMaterial(Material material) throws Throwable {
        return setObject(Properties.Surface.PARAM_MATERIAL, DataType.MATERIAL, material);
    }

    /**
     * Whether the surface is visible. Default: TRUE.
     */
    public Surface setVisible(boolean visible) throws Throwable {
        return setBool(Properties.Surface.PARAM_VISIBLE, visible);
    }

    /**
     * Optional user id, for frame channel objectId. Default: -1u.
     * <br>
     * Extension KHR_FRAME_CHANNEL_OBJECT_ID
     */
    public Surface setId(int id) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Surface.PARAM_ID, DataType.UINT32, tempArena.allocateFrom(JAVA_INT, id));
        }
    }
}
