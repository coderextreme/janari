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
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import java.util.List;

/**
 * A renderer is the central object for rendering in ANARI. Different renderers
 * implement different extensions, rendering algorithms, and support different
 * materials. Every ANARI device offers a default renderer, which works without
 * setting any parameters.
 *
 * {@snippet lang=c :
 * typedef void* ANARIRenderer;
 * }
 *
 * @author Johann Sorel
 */
public final class Renderer extends Object<Renderer> {

    public Renderer(Device device, MemorySegment pointer) {
        super(device, DataType.RENDERER, null, pointer);
    }

    /**
     * List of extensions supported by this renderer, queried as a property.
     */
    public List<String> getExtension() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(ADDRESS);
            getProperty(Properties.Renderer.QUERY_EXTENSION, DataType.STRING_LIST, mem, mem.byteSize());
            return AnariOO.fromNullTerminatedStrings(mem.get(ADDRESS, 0));
        } finally {
            device.checkForException();
        }
    }

    /**
     * The background color. Default: (0, 0, 0, 1).
     * <br>
     * Extension KHR_RENDERER_BACKGROUND_COLOR
     */
    public Renderer setBackground(float r, float g, float b, float a) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Renderer.PARAM_BACKGROUND, DataType.FLOAT32_VEC4, tempArena.allocateFrom(JAVA_FLOAT, r, g, b, a));
        }
    }

    /**
     * The background image, rescaled to the size of the Frame by linear filtering.
     * <br>
     * Extension KHR_RENDERER_BACKGROUND_IMAGE
     */
    public Renderer setBackground(Array2D image) throws AnariException, Throwable {
        return setObject(Properties.Renderer.PARAM_BACKGROUND, DataType.ARRAY2D, image);
    }

    /**
     * Ambient light color. The ambient light is a light with an invisible source
     * which surrounds the scene and illuminates it from infinity. Default: (1, 1, 1).
     * <br>
     * Extension KHR_RENDERER_AMBIENT_LIGHT
     */
    public Renderer setAmbientColor(float r, float g, float b) throws AnariException, Throwable {
        return setFloat32Vec3(Properties.Renderer.PARAM_AMBIENTCOLOR, r,g,b);
    }

    /**
     * The amount of light emitted by a point on the ambient light source in a
     * direction, in W/sr/m2. Default: 0.
     * <br>
     * Extension KHR_RENDERER_AMBIENT_LIGHT
     */
    public Renderer setAmbientRadiance(float ambientRadiance) throws AnariException, Throwable {
        return setFloat32(Properties.Renderer.PARAM_AMBIENTRADIANCE, ambientRadiance);
    }

    /**
     * Whether the rendered image should be denoised. Default: FALSE.
     * <br>
     * Extension KHR_RENDERER_DENOISE
     */
    public Renderer setDenoise(boolean denoise) throws AnariException, Throwable {
        return setBool(Properties.Renderer.PARAM_DENOISE, denoise);
    }
}
