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
package org.codeberg.anari.javafx;

import java.lang.foreign.Arena;
import javafx.beans.value.ObservableValue;
import javafx.event.EventType;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.codeberg.anari.api.Camera;
import static org.codeberg.anari.api.DataType.UFIXED8_RGBA_SRGB;
import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Frame;
import org.codeberg.anari.api.Renderer;
import org.codeberg.anari.api.WaitMask;
import org.codeberg.anari.api.World;

/**
 * Abstract handler with EXAMINE navigation support.
 * Subclasses can override getViewpoint() to provide X3D Viewpoint data.
 */
public class AbstractHandler extends AnariHandler {

    //last rendering state
    private int width;
    private int height;

    //cache anari objects
    protected Arena arena;
    protected Renderer renderer;
    protected Camera.Perspective camera;
    protected Frame frame;
    protected World world;

    // EXAMINE navigation state
    protected float cameraDistance = 15.0f;
    protected float cameraAzimuth = 0.0f;   // rotation around Y axis (degrees)
    protected float cameraElevation = 20.0f; // rotation above XZ plane (degrees)
    protected float[] cameraTarget = {0.0f, 0.0f, 0.0f}; // look-at point
    
    // mouse drag tracking
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean mouseDragging = false;

    @Override
    public void initialize(AnariPane pane) {
        super.initialize(pane);
        pane.addEventHandler(MouseEvent.ANY, this::mouseChange);
        pane.addEventHandler(KeyEvent.ANY, this::keyChange);
        pane.addEventHandler(ScrollEvent.ANY, this::scrollChange);
        pane.widthProperty().addListener(this::sizeChanged);
        pane.heightProperty().addListener(this::sizeChanged);
    }

    @Override
    public void initialize(Device device) {
        super.initialize(device);
        arena = Arena.global();
    }

    @Override
    public void release(Device device) {
        arena.close();
        arena = null;

        try {
            renderer.release();
            camera.release();
            frame.release();
            world.release();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        arena = null;
        renderer = null;
        camera = null;
        frame = null;
        world = null;

        super.release(device);
    }

    protected void sizeChanged(ObservableValue ov, Object t, Object t1) {
        requestRepaint();
    }

    protected void mouseChange(MouseEvent event) {
        EventType<? extends MouseEvent> type = event.getEventType();
        
        if (type == MouseEvent.MOUSE_PRESSED) {
            if (event.isPrimaryButtonDown()) {
                mouseDragging = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                pane.requestFocus();
            }
        } else if (type == MouseEvent.MOUSE_RELEASED) {
            mouseDragging = false;
        } else if (type == MouseEvent.MOUSE_DRAGGED && mouseDragging) {
            double dx = event.getX() - lastMouseX;
            double dy = event.getY() - lastMouseY;
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            
            // Left drag: orbit (azimuth/elevation)
            cameraAzimuth -= (float) dx * 0.5f;
            cameraElevation = Math.max(-89.0f, Math.min(89.0f, cameraElevation - (float) dy * 0.5f));
            requestRepaint();
        } else if (type == MouseEvent.MOUSE_ENTERED) {
            pane.requestFocus();
        }
    }

    protected void scrollChange(ScrollEvent event) {
        double deltaY = event.getDeltaY();
        // Scroll: zoom in/out
        cameraDistance = Math.max(1.0f, Math.min(100.0f, cameraDistance - (float) deltaY * 0.1f));
        requestRepaint();
    }

    protected void keyChange(KeyEvent event) {
        EventType<KeyEvent> eventType = event.getEventType();
        if (eventType == KeyEvent.KEY_TYPED) {
            // Could add key bindings here (e.g., R to reset view)
        }
        requestRepaint();
    }

    /** Override to provide Viewpoint from X3D scene. Returns null to use defaults. */
    protected ViewpointData getViewpoint() {
        return null;
    }

    @Override
    public Frame repaint(TimerState timer, int width, int height) throws Throwable {

        // Initialize camera from Viewpoint if available, else use EXAMINE defaults
        if (camera == null) {
            ViewpointData vp = getViewpoint();
            float[] pos, dir, up;
            float fovy = 45.0f;
            
            if (vp != null) {
                pos = vp.position;
                // Compute direction from camera position to target (look-at)
                // This ensures camera points at the scene center regardless of Viewpoint orientation
                float dx = cameraTarget[0] - pos[0];
                float dy = cameraTarget[1] - pos[1];
                float dz = cameraTarget[2] - pos[2];
                float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
                dir = new float[]{dx/len, dy/len, dz/len};
                up = vp.up;
                if (vp.fieldOfView > 0) fovy = (float) Math.toDegrees(vp.fieldOfView);
                System.out.println("Camera from Viewpoint: pos=" + java.util.Arrays.toString(pos) + 
                    ", target=" + java.util.Arrays.toString(cameraTarget) + 
                    ", computed dir=" + java.util.Arrays.toString(dir) + 
                    ", len=" + len);
            } else {
                // EXAMINE mode: compute camera position from spherical coordinates
                double az = Math.toRadians(cameraAzimuth);
                double el = Math.toRadians(cameraElevation);
                float cx = cameraTarget[0] + cameraDistance * (float) (Math.cos(el) * Math.sin(az));
                float cy = cameraTarget[1] + cameraDistance * (float) Math.sin(el);
                float cz = cameraTarget[2] + cameraDistance * (float) (Math.cos(el) * Math.cos(az));
                pos = new float[]{cx, cy, cz};
                // Compute normalized direction from camera to target
                float dx = cameraTarget[0] - cx;
                float dy = cameraTarget[1] - cy;
                float dz = cameraTarget[2] - cz;
                float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
                dir = new float[]{dx/len, dy/len, dz/len};
                up = new float[]{0.0f, 1.0f, 0.0f};
                System.out.println("EXAMINE camera: pos=" + java.util.Arrays.toString(pos) + 
                    ", target=" + java.util.Arrays.toString(cameraTarget) + 
                    ", dir=" + java.util.Arrays.toString(dir) + 
                    ", distance=" + cameraDistance + ", az=" + cameraAzimuth + ", el=" + cameraElevation);
            }

            camera = device.newCamera(Camera.SubType.PERSPECTIVE)
                .setAspect((float) width / (float) height)
                .setPosition(pos[0], pos[1], pos[2])
                .setDirection(dir[0], dir[1], dir[2])
                .setUp(up[0], up[1], up[2])
                .setFovY((float) Math.toRadians(fovy))
                .setNear(0.1f)
                .setFar(100.0f)
                .commit();
        }

        if (renderer == null) {
            renderer = device.newRenderer("default")
                .setBackground(1.0f, 0.0f, 0.0f, 1.0f) // Bright red background to verify rendering
                .commit();
        }

        if (world == null) {
            world = device.newWorld()
                    .commit();
        }

        updateScene(timer);

        // Update camera for EXAMINE mode if no Viewpoint provided
        if (getViewpoint() == null && camera != null) {
            double az = Math.toRadians(cameraAzimuth);
            double el = Math.toRadians(cameraElevation);
            float cx = cameraTarget[0] + cameraDistance * (float) (Math.cos(el) * Math.sin(az));
            float cy = cameraTarget[1] + cameraDistance * (float) Math.sin(el);
            float cz = cameraTarget[2] + cameraDistance * (float) (Math.cos(el) * Math.cos(az));
            camera.setPosition(cx, cy, cz)
                  .setDirection(cameraTarget[0] - cx, cameraTarget[1] - cy, cameraTarget[2] - cz)
                  .commit();
        }

        // Create a new frame each repaint (like OffscreenRenderingDemo) to avoid
        // issues with frame reuse and stale state.
        if (frame != null) {
            frame.release();
        }
        frame = device.newFrame()
            .setSize(width, height)
            .setChannelColor(UFIXED8_RGBA_SRGB)
            .setRenderer(renderer)
            .setCamera(camera)
            .setWorld(world)
            .commit();

System.out.println("About to render frame...");
        frame.render();
        System.out.println("frame.render() returned");

        // Wait for frame to complete before mapping (helide works with WAIT)
        int ready = frame.ready(WaitMask.WAIT);
        System.out.println("frame.ready(WAIT) returned: " + ready);

        System.out.println("done!\n");
        return frame;
    }

    protected void updateScene(TimerState timer) throws Throwable {
    }

    /** Simple container for Viewpoint data from X3D scene */
    public static class ViewpointData {
        public float[] position = {0, 0, 10};
        public float[] direction = {0, 0, -1};
        public float[] up = {0, 1, 0};
        public double fieldOfView = 0; // radians, 0 = use default
    }

    @Override
    public java.lang.foreign.Arena getArena() {
        return arena;
    }

}
