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

/**
 * Worlds are a container of scene data, the root object of the scene. Objects
 * are placed in the world through arrays of instances, surfaces, volumes, or
 * lights; each array is optional.
 *
 * {@snippet lang=c :
 * typedef void* ANARIWorld;
 * }
 *
 * @author Johann Sorel
 */
public final class World extends Object<World> {

    public World(Device device, MemorySegment pointer) {
        super(device, DataType.WORLD, null, pointer);
    }

    /**
     * Axis-aligned bounding box in world-space (excluding the lights), queried as a property.
     */
    public float[] getBounds() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment worldBounds = tempArena.allocate(6*4);
            final int res = getProperty(Properties.World.QUERY_BOUNDS, DataType.FLOAT32_BOX3, worldBounds, 6*4, WaitMask.WAIT);
            if (res == 0) return null;
            final float[] wbf = new float[6];
            worldBounds.asByteBuffer().asFloatBuffer().get(wbf);
            return wbf;
        }
    }

    /**
     * Optional array with handles of instances.
     */
    public World setInstance(Array1D instance) throws AnariException, Throwable {
        return setObject(Properties.World.PARAM_INSTANCE, DataType.ARRAY1D, instance);
    }

    /**
     * Optional array with handles of surfaces.
     */
    public World setSurface(Array1D surface) throws AnariException, Throwable {
        return setObject(Properties.World.PARAM_SURFACE, DataType.ARRAY1D, surface);
    }

    /**
     * Optional array with handles of volumes.
     */
    public World setVolume(Array1D volume) throws AnariException, Throwable {
        return setObject(Properties.World.PARAM_VOLUME, DataType.ARRAY1D, volume);
    }

    /**
     * Optional array with handles of lights.
     */
    public World setLight(Array1D light) throws AnariException, Throwable {
        return setObject(Properties.World.PARAM_LIGHT, DataType.ARRAY1D, light);
    }
}
