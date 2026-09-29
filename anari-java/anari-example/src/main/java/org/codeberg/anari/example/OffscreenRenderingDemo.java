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

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static java.lang.foreign.MemorySegment.NULL;
import java.lang.foreign.ValueLayout;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.codeberg.anari.Anari;
import org.codeberg.anari.api.AnariOO;
import org.codeberg.anari.api.Array1D;
import org.codeberg.anari.api.Camera;
import org.codeberg.anari.api.DataType;
import static org.codeberg.anari.api.DataType.*;
import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Frame;
import org.codeberg.anari.api.Geometry;
import org.codeberg.anari.api.Library;
import org.codeberg.anari.api.Light;
import org.codeberg.anari.api.Material;
import org.codeberg.anari.api.Parameter;
import org.codeberg.anari.api.Renderer;
import org.codeberg.anari.api.Surface;
import org.codeberg.anari.api.WaitMask;
import org.codeberg.anari.api.World;

/**
 * Java port of
 * https://github.com/KhronosGroup/ANARI-SDK/blob/next_release/examples/simple/anariTutorial.c
 *
 *
 * @author Johann Sorel
 */
public class OffscreenRenderingDemo {

    public static void main(String[] args) throws Throwable {
        new OffscreenRenderingDemo();
    }

    private final Arena arena = Arena.global();

    public OffscreenRenderingDemo() throws Throwable {

        final Path path = Path.of("/home/yottzumm/janari/ANARI-SDK-0.16.0/build/libanari.so");

        final Anari anari = Anari.load(path);

        // image size
        final int[] imgSize = new int[]{1024, 768};

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

        System.out.println("initialize ANARI...");

        // Use the 'helide' library here, this is where the impl(s) come from
        final Library library = new AnariOO(anari, arena).loadLibrary("helide", null);

        // the remaining queries have to use a device
        final Device device = library.newDevice("default")
                .commit();

        System.out.println("done!\n");
        System.out.println("setting up camera...");

        // create and setup camera
        final Camera.Perspective camera = device.newCamera(Camera.SubType.PERSPECTIVE)
                .setAspect((float) imgSize[0] / (float) imgSize[1])
                .setPosition(0.0f, 0.0f, 0.0f)
                .setDirection(0.1f, 0.0f, 1.0f)
                .setUp(0.0f, 1.0f, 0.0f)
                .commit(); // commit each object to indicate mods are done

        System.out.println("done!\n");
        System.out.println("setting up scene...");

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

        // The world to be populated with renderable objects
        final World world = device.newWorld()
                .setSurface(arraySurfaces)
                .setLight(arrayLights)
                .commit();

        surface.release();
        arraySurfaces.release();
        light.release();
        arrayLights.release();

        System.out.println("done!\n");

        // print out world bounds
        float[] bounds = world.getBounds();
        if (bounds != null) {
            System.out.printf("\nworld bounds: ({%f, %f, %f}, {%f, %f, %f}\n\n",
                    bounds[0],
                    bounds[1],
                    bounds[2],
                    bounds[3],
                    bounds[4],
                    bounds[5]);
        } else {
            System.out.println("\nworld bounds not returned\n\n");
        }

        System.out.println("setting up renderer...");

        // create renderer
        final Renderer renderer = device.newRenderer("default")
                .setBackground(0, 0, 0, 0)
                .commit();

        // create and setup frame
        final Frame frame = device.newFrame()
                .setSize(imgSize[0], imgSize[1])
                .setChannelColor(UFIXED8_RGBA_SRGB)
                .setRenderer(renderer)
                .setCamera(camera)
                .setWorld(world)
                .commit();

        System.out.println("rendering initial frame to firstFrame.ppm...");

        // render one frame
        frame.render();
        frame.ready(WaitMask.WAIT);

        System.out.println("done!\n");
        System.out.println("rendering 10 accumulated frames to accumulatedFrame.ppm...");

        // render 10 more frames, which are accumulated to result in a better
        //   converged image
        for (int frames = 0; frames < 1; frames++) {
            frame.render();
            frame.ready(WaitMask.WAIT);
        }

        writePNG(frame, 1024, 768);

        System.out.println("done!\n");
        System.out.println("\ncleaning up objects...");

        // final cleanups
        renderer.release();
        camera.release();
        frame.release();
        world.release();

        device.release();

        System.out.println(printDescription(library));

        library.unload();

        System.out.println("done!\n");

    }

    private String printDescription(Library library) throws Throwable {

        // query available devices
        final List<String> devices = library.getDeviceSubtypes();
        if (devices.isEmpty()) {
            System.out.println("No devices anounced.");
        } else {
            System.out.println("Available devices:");
            for (String d : devices) {
                System.out.println("  - " + d);
                Device device = library.newDevice(d);
                printDescription(device);
                device.release();
            }
        }
        return "";
    }

    private String printDescription(Device device) throws Throwable {

        final DataType[] types = new DataType[]{RENDERER, CAMERA, FRAME, GEOMETRY, GROUP, INSTANCE, LIGHT, MATERIAL, SAMPLER, SURFACE, VOLUME, WORLD};

        // query available renderers
        for (DataType dt : types) {
            System.out.println(dt.name());

            final List<String> candidates = device.getObjectSubtypes(dt);

            if (candidates == null) {
                String candidateName = null;
                final List<Parameter> params = Parameter.toList(device.getObjectInfo(dt, candidateName, "parameter", PARAMETER_LIST));
                for (Parameter p : params) {
                    String desc = AnariOO.toString(device.getParameterInfo(
                            dt,
                            candidateName,
                            p.getName(),
                            p.getType(),
                            "description",
                            STRING));
                    int required = device.getParameterInfo(
                            dt,
                            candidateName,
                            p.getName(),
                            p.getType(),
                            "required",
                            BOOL).reinterpret(4).get(ValueLayout.JAVA_INT, 0);
                    System.out.printf("    - %s [%s] : %s: %s\n",
                            p.getName(),
                            p.getType().toString(),
                            required != 0 ? "required" : "optional",
                            desc);
                }
            }

            for (String candidateName : candidates) {
                System.out.println("  - " + candidateName);

                final List<Parameter> params = Parameter.toList(device.getObjectInfo(dt, candidateName, "parameter", PARAMETER_LIST));
                for (Parameter p : params) {
                    String desc = AnariOO.toString(device.getParameterInfo(
                            dt,
                            candidateName,
                            p.getName(),
                            p.getType(),
                            "description",
                            STRING));
                    int required = device.getParameterInfo(
                            dt,
                            candidateName,
                            p.getName(),
                            p.getType(),
                            "required",
                            BOOL).reinterpret(4).get(ValueLayout.JAVA_INT, 0);
                    System.out.printf("    - %s [%s] : %s: %s\n",
                            p.getName(),
                            p.getType().toString(),
                            required != 0 ? "required" : "optional",
                            desc);
                }

            }
        }

        return "";
    }

    private void writePNG(Frame frame, int width, int height) throws Throwable {
        MemorySegment mem = frame.map(
                arena,
                "channel.color",
                width,
                height,
                UNKNOWN);
        final byte[] datas = new byte[width * height * 4];
        MemorySegment reinterpret = mem.reinterpret(datas.length);
        reinterpret.asByteBuffer().get(datas);
        frame.unmap(arena, "channel.color");

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int offset = (y * width + x) * 4;
                image.getRaster().setSample(x, y, 0, datas[offset + 0] & 0xFF);
                image.getRaster().setSample(x, y, 1, datas[offset + 1] & 0xFF);
                image.getRaster().setSample(x, y, 2, datas[offset + 2] & 0xFF);
                image.getRaster().setSample(x, y, 3, datas[offset + 3] & 0xFF);
            }
        }
        ImageIO.write(image, "png", new File("anari.png"));
    }
}
