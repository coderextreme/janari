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
import java.lang.foreign.MemorySegment;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import org.codeberg.anari.Anari;
import org.codeberg.anari.api.AnariOO;
import static org.codeberg.anari.api.DataType.UFIXED8_RGBA_SRGB;
import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Frame;
import org.codeberg.anari.api.Library;

/**
 *
 * @author Johann Sorel
 */
public final class AnariPane extends StackPane {

    private static final Path path = Path.of("/tmp/anari-sdk/install/lib64/libanari.so");
    private static final Anari ANARI = Anari.load(path);

    private final ImageView imageView = new ImageView();
    private final ExecutorService service = Executors.newFixedThreadPool(1);

    private final AtomicBoolean needRepaint = new AtomicBoolean(false);
    private final TimerState timer = new TimerState();

    private Library library;
    private Device device;
    private AnariHandler handler = null;


    public AnariPane() {
        getChildren().add(imageView);
    }

    public void setHandler(AnariHandler handler) throws Throwable {
        if (this.library == null) {
            // Load library from ANARI_LIBRARY env var (helide, helide_gpu, visgl)
            String libraryName = System.getenv("ANARI_LIBRARY");
            if (libraryName == null || libraryName.isEmpty()) {
                libraryName = "helide";
            }
            System.out.println("Loading ANARI library: " + libraryName);
            Arena arena = Arena.global();

            // Previously passed `null` here, which meant we had no visibility
            // into anything the native library reported. renderFrame() is
            // documented as asynchronous -- the actual render work happens on
            // a separate native thread -- so if that thread hits an error, it
            // can invoke this callback at any time, independently of whatever
            // our main thread is currently blocked inside (frameReady/mapFrame
            // were both observed to hang indefinitely). This callback fires as
            // a native upcall regardless, so it's our only way to see that.
            org.codeberg.anari.api.StatusCallback statusLogger = new org.codeberg.anari.api.StatusCallback() {
                @Override
                public void report(Device dev, org.codeberg.anari.api.Object source,
                                    org.codeberg.anari.api.DataType sourceType,
                                    org.codeberg.anari.api.Severity severity,
                                    org.codeberg.anari.api.StatusCode code,
                                    String message) {
                    System.out.println("[ANARI STATUS] severity=" + severity + " code=" + code +
                        " sourceType=" + sourceType + " message=" + message);
                }
            };
            library = new AnariOO(ANARI, arena).loadLibrary(libraryName, statusLogger);

            // Use "default" device like OffscreenRenderingDemo (works with helide)
            device = library.newDevice("default").commit();
        }

        this.handler = handler;
        this.handler.initialize(this);
        this.handler.initialize(device);
    }

    void repaint() {
        needRepaint.set(true);
        service.submit(new RepaintTask());
    }

    private class RepaintTask implements Runnable {

        @Override
        public void run() {
            boolean old = needRepaint.getAndSet(false);
            if (!old) return;

            System.out.println("RepaintTask: starting on thread " + Thread.currentThread());
            WritableImage image;
            try {
                image = anariPaint();
                System.out.println("RepaintTask: anariPaint() returned " + image);
                if (image != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImage(image);
                            System.out.println("RepaintTask: imageView.setImage() applied on FX thread");
                        }
                    });
                }
            } catch (Throwable ex) {
                System.out.println("RepaintTask: anariPaint() threw " + ex);
                ex.printStackTrace();
            }
            System.out.println("RepaintTask: finished");
        }

    }

    private WritableImage anariPaint() throws Throwable {
        System.out.println("anariPaint() called");
        if (handler == null) return null;

        final int width = (int) getWidth();
        final int height = (int) getHeight();
        System.out.println("anariPaint: width=" + width + ", height=" + height);
        timer.pulse();
        Frame frame = handler.repaint(timer, width, height);
        System.out.println("handler.repaint returned frame: " + frame);
        return toImage(frame, width, height, handler.getArena());


//        device.release();
//        library.unload();
    }


    private static WritableImage toImage(Frame frame, int width, int height, Arena arena) throws Throwable {
        System.out.println("toImage: about to call frame.map(...)");

        // Watchdog: if frame.map() hangs (as frame.ready(WAIT) previously did),
        // this prints a loud warning after 5s instead of leaving you guessing
        // whether it's slow or stuck. Does not cancel the call -- native FFM
        // calls generally can't be interrupted safely -- just makes a hang visible.
        final Thread callingThread = Thread.currentThread();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(5000);
                System.out.println("WARNING: frame.map(...) has not returned after 5000ms -- " +
                    "likely hanging in the native/FFM call, same family of issue as the earlier " +
                    "frame.ready(WaitMask.WAIT) hang. Thread stack:");
                for (StackTraceElement el : callingThread.getStackTrace()) {
                    System.out.println("    at " + el);
                }
            } catch (InterruptedException ignored) {
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();

        MemorySegment mem = frame.map(
                arena,
                "channel.color",
                width,
                height,
                UFIXED8_RGBA_SRGB);
        watchdog.interrupt();
        System.out.println("toImage: frame.map(...) returned, mem=" + mem);

        final byte[] datas = new byte[width * height * 4];
        MemorySegment reinterpret = mem.reinterpret(datas.length);
        reinterpret.asByteBuffer().get(datas);
        System.out.println("toImage: about to call frame.unmap(...)");
        frame.unmap(arena, "channel.color");
        System.out.println("toImage: frame.unmap(...) returned");

        // Debug: check first few pixels
        System.out.println("Frame data: first 16 bytes = " + java.util.Arrays.toString(java.util.Arrays.copyOf(datas, 16)) + 
            ", width=" + width + ", height=" + height);
        // Check if any non-zero pixels
        int nonZero = 0;
        for (byte b : datas) if (b != 0) nonZero++;
        System.out.println("Non-zero bytes: " + nonZero + " / " + datas.length);

        Buffer buffer = ByteBuffer.wrap(datas);
        final PixelBuffer pb = new PixelBuffer(width, height, buffer, PixelFormat.getByteBgraPreInstance());

        final WritableImage image = new WritableImage(pb);
        return image;
    }

}
