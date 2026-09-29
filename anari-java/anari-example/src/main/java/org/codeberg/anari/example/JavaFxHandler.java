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
package org.codeberg.anari.example;

import static java.lang.foreign.MemorySegment.NULL;
import org.codeberg.anari.api.Array1D;
import static org.codeberg.anari.api.DataType.FLOAT32_VEC3;
import static org.codeberg.anari.api.DataType.FLOAT32_VEC4;
import static org.codeberg.anari.api.DataType.LIGHT;
import static org.codeberg.anari.api.DataType.SURFACE;
import static org.codeberg.anari.api.DataType.UINT32_VEC3;
import org.codeberg.anari.api.Geometry;
import org.codeberg.anari.api.Light;
import org.codeberg.anari.api.Material;
import org.codeberg.anari.api.Surface;
import org.codeberg.anari.javafx.AbstractHandler;
import org.codeberg.anari.javafx.TimerState;

/**
 *
 * @author Johann Sorel
 */
public class JavaFxHandler extends AbstractHandler {

    @Override
    protected void updateScene(TimerState timer) throws Throwable {

        // triangle mesh data
        final float[] vertex = new float[]{
            -1.0f, -1.0f, 3.0f,
            -1.0f, 1.0f, 3.0f,
            1.0f, -1.0f, 3.0f,
            0.1f, 0.1f, 0.3f
        };
        final float[] color = new float[]{
            0.9f, 0.5f, 0.5f, 1.0f, // red
            0.8f, 0.8f, 0.8f, 1.0f, // 80% gray
            0.8f, 0.8f, 0.8f, 1.0f, // 80% gray
            0.5f, 0.9f, 0.5f, 1.0f // green
        };
        final int[] index = new int[]{
            0, 1, 2, // triangle-1
            1, 2, 3 // triangle-2
        };

        // create and setup surface and mesh
        //   A mesh requires an index, plus arrays of: locations & colors
        // Set the vertex locations
        final Array1D arrayPos = device.newArray1D(FLOAT32_VEC3, 4)
                .set(true, vertex)
                .commit();

        // Set the vertex colors
        final Array1D arrayColors = device.newArray1D(FLOAT32_VEC4, 4)
                .set(true, color)
                .commit();

        // Set the index
        final Array1D arrayIndex = device.newArray1D(UINT32_VEC3, 2)
                .set(true, index)
                .commit();

        // Affect all the mesh values
        final Geometry.Triangle mesh = device.newGeometry(Geometry.SubType.TRIANGLE)
                .setVertexPosition(arrayPos)
                .setVertexColor(arrayColors)
                .setPrimitiveIndex(arrayIndex)
                .commit();
        arrayPos.release();
        arrayColors.release();
        arrayIndex.release();

        // Set the material rendering parameters
        final Material mat = device.newMaterial(Material.SubType.MATTE)
            .setColor("color")
            .commit();

        // put the mesh into a surface
        final Surface surface = device.newSurface()
                .setGeometry(mesh)
                .setMaterial(mat)
                .commit();
        mesh.release();
        mat.release();

        // put the surface directly onto the world
        final Array1D arraySurfaces = device.newArray1D(surface.getAddress(arena), NULL, NULL, SURFACE, 1)
                .commit();

        // create and setup light for Ambient Occlusion
        final Light light = device.newLight(Light.SubType.DIRECTIONAL)
                .commit();
        final Array1D arrayLights = device.newArray1D(light.getAddress(arena), NULL, NULL, LIGHT, 1)
                .commit();

        world.setSurface(arraySurfaces)
             .setLight(arrayLights)
             .commit();

        surface.release();
        arraySurfaces.release();
        light.release();
        arrayLights.release();
    }


}
