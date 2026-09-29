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
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * The frame contains all the objects necessary to render and holds the resulting
 * rendered 2D image (and optionally auxiliary information associated with pixels).
 *
 * The frame uses parameters to encode size, color format, and which channels to use.
 * Channels are identified by string channel names beginning with channel..
 * The frame object has an identically named parameter for each reported channel.
 * Setting these parameters to one of the allowed data types enables the channel
 * and determines the data type it can be mapped as. The same channel names are
 * used to map the channel contents with anariMapFrame.
 * Each channel has its own associated extension name.
 *
 * {@snippet lang=c :
 * typedef void* ANARIFrame;
 * }
 *
 * @author Johann Sorel
 */
public final class Frame extends Object<Frame> {

    public Frame(Device device, MemorySegment pointer) {
        super(device, DataType.FRAME, null, pointer);
    }

    /**
     * Set required world to be rendered.
     */
    public Frame setWorld(World world) throws Throwable {
        return setObject(Properties.Frame.PARAM_WORLD, DataType.WORLD, world);
    }

    /**
     * Set required camera used to render the world.
     */
    public Frame setCamera(Camera camera) throws Throwable {
        return setObject(Properties.Frame.PARAM_CAMERA, DataType.CAMERA, camera);
    }

    /**
     * Set required renderer which renders the frame.
     */
    public Frame setRenderer(Renderer renderer) throws Throwable {
        return setObject(Properties.Frame.PARAM_RENDERER, DataType.RENDERER, renderer);
    }

    /**
     * Set required size of the frame in pixels (width × height).
     */
    public Frame setSize(int width, int height) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_SIZE, DataType.UINT32_VEC2, tempArena.allocateFrom(JAVA_INT, width, height));
        }
    }

    /**
     * Set to true to accumulate result on the frame.
     */
    public Frame setAccumulation(boolean acc) throws Throwable {
        return setBool(Properties.Frame.PARAM_ACCUMULATION, acc);
    }

    /**
     * Enable mapping the color channel and specify its observable type;
     * RGB color including alpha;
     * possible values: UFIXED8_VEC4, UFIXED8_RGBA_SRGB, FLOAT32_VEC4
     */
    public Frame setChannelColor(DataType datatype) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_CHANNEL_COLOR, DataType.DATA_TYPE, tempArena.allocateFrom(JAVA_INT, datatype.code));
        }
    }

    /**
     * Enable mapping the depth channel and specify its observable type;
     * euclidean distance to the camera (not to the image plane), for multiple samples per pixel their minimum is taken;
     * possible values: FLOAT32
     */
    public Frame setChannelDepth(DataType datatype) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_CHANNEL_DEPTH, DataType.DATA_TYPE, tempArena.allocateFrom(JAVA_INT, datatype.code));
        }
    }

    /**
     * With extension : KHR_FRAME_CHANNEL_NORMAL
     *
     * Enable mapping the normal channel and specify its observable type;
     * average world-space normal of the first hit;
     * possible values: FIXED16_VEC3, FLOAT32_VEC3
     */
    public Frame setChannelNormal(DataType datatype) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_CHANNEL_NORMAL, DataType.DATA_TYPE, tempArena.allocateFrom(JAVA_INT, datatype.code));
        }
    }

    /**
     * With extension : KHR_FRAME_CHANNEL_ALBEDO
     *
     * Enable mapping the albedo channel and specify its observable type;
     * average material albedo (color without illumination) at the first hit;
     * possible values: UFIXED8_VEC3, UFIXED8_RGB_SRGB, FLOAT32_VEC3
     */
    public Frame setChannelAlbedo(DataType datatype) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_CHANNEL_ALBEDO, DataType.DATA_TYPE, tempArena.allocateFrom(JAVA_INT, datatype.code));
        }
    }

    /**
     * With extension : KHR_FRAME_CHANNEL_PRIMITIVE_ID
     *
     * Enable mapping the primitiveId channel and specify its observable type;
     * primitiveId attribute of the first hit;
     * possible values: UINT32
     */
    public Frame setChannelPrimitiveId(DataType datatype) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_CHANNEL_PRIMITIVEID, DataType.DATA_TYPE, tempArena.allocateFrom(JAVA_INT, datatype.code));
        }
    }

    /**
     * With extension : KHR_FRAME_CHANNEL_OBJECT_ID
     *
     * Enable mapping the objectId channel and specify its observable type;
     * user defined Surface / Volume id, if specified, or index in Group of first hit;
     * possible values: UINT32
     */
    public Frame setChannelObjectId(DataType datatype) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_CHANNEL_OBJECTID, DataType.DATA_TYPE, tempArena.allocateFrom(JAVA_INT, datatype.code));
        }
    }

    /**
     * With extension : KHR_FRAME_CHANNEL_INSTANCE_ID
     *
     * Enable mapping the instanceId channel and specify its observable type;
     * user defined Instance id, if specified, or instance index of first hit;
     * possible values: UINT32
     */
    public Frame setChannelInstanceId(DataType datatype) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return set(Properties.Frame.PARAM_CHANNEL_INSTANCEID, DataType.DATA_TYPE, tempArena.allocateFrom(JAVA_INT, datatype.code));
        }
    }

    /**
     * With extension : KHR_FRAME_COMPLETION_CALLBACK
     *
     * Set the callback to invoke as a continuation when the rendered frame is
     * complete. Implementations are strongly encouraged to invoke the
     * continuation on a background thread. The callback function pointer is
     * kept alive for the lifetime of the JVM.
     */
    public Frame setCompletionCallback(FrameCompletionCallback callback) throws Throwable {
        final MemorySegment funcPtr = device.library.anaripoo.toNativeFunctionReference(callback, Arena.global());
        return set(Properties.Frame.PARAM_COMPLETIONCALLBACK, DataType.FRAME_COMPLETION_CALLBACK, funcPtr);
    }

    /**
     * With extension : KHR_FRAME_COMPLETION_CALLBACK
     *
     * Set the optional user pointer passed as the first argument of the frame
     * completion callback.
     */
    public Frame setCompletionCallbackUserData(MemorySegment userData) throws Throwable {
        return set(Properties.Frame.PARAM_COMPLETIONCALLBACKUSERDATA, DataType.VOID_POINTER, userData);
    }

    /**
     * With extension : KHR_DATA_PARALLEL_MPI
     *
     * Set the MPI communicator (an {@code MPI_Comm}, passed as a raw pointer)
     * which the device should treat as the MPI world. Default: MPI_COMM_WORLD.
     */
    public Frame setMpiCommunicator(MemorySegment mpiCommunicator) throws Throwable {
        return set(Properties.Frame.PARAM_MPICOMMUNICATOR, DataType.VOID_POINTER, mpiCommunicator);
    }

    /**
     * Time (in seconds) between start and completion of the frame, queried as a
     * mandatory property.
     */
    public float getDuration() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_FLOAT);
            getProperty(Properties.Frame.QUERY_DURATION, DataType.FLOAT32, mem, mem.byteSize());
            return mem.get(JAVA_FLOAT, 0);
        }
    }

    /**
     * Progress of the current frame task since the last call to render(), in
     * [0..1], queried as an optional property. Returns null if not available.
     */
    public Float getRenderProgress() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_FLOAT);
            final int res = getProperty(Properties.Frame.QUERY_RENDERPROGRESS, DataType.FLOAT32, mem, mem.byteSize(), WaitMask.NO_WAIT);
            if (res == 0) return null;
            return mem.get(JAVA_FLOAT, 0);
        }
    }

    /**
     * With extension : KHR_FRAME_ACCUMULATION
     *
     * Progress of frame refinement, in [0..1], queried as an optional property.
     * Returns null if not available.
     */
    public Float getRefinementProgress() throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(JAVA_FLOAT);
            final int res = getProperty(Properties.Frame.QUERY_REFINEMENTPROGRESS, DataType.FLOAT32, mem, mem.byteSize(), WaitMask.NO_WAIT);
            if (res == 0) return null;
            return mem.get(JAVA_FLOAT, 0);
        }
    }

    /**
     * Trigger an asynchronous render of this frame. This call may not block; use
     * {@link #ready(WaitMask)} to synchronize with the application.
     */
    public void render() throws Throwable {
        try {
            device.library.anari.renderFrame(device.getPointer(), getPointer());
        } finally {
            device.checkForException();
        }
    }

    /**
     * Query the status of, or wait on, this running frame. With {@link WaitMask#NO_WAIT}
     * returns true if the frame has completed. With {@link WaitMask#WAIT} blocks
     * the calling thread until the frame has completed and always returns true.
     */
    public int ready(WaitMask mask) throws AnariException, Throwable {
        try {
            return (int) device.library.anari.frameReady(device.getPointer(), getPointer(), mask.code);
        } finally {
            device.checkForException();
        }
    }

    /**
     * Signal that this in-flight frame should be canceled if possible. Not
     * required to block until the frame completes; the contents of a mapped
     * frame which has been discarded is undefined.
     */
    public void discard() throws AnariException, Throwable {
        try {
            device.library.anari.discardFrame(device.getPointer(), getPointer());
        } finally {
            device.checkForException();
        }
    }

    /**
     * {@snippet lang=c :
     * const void* anariMapFrame(ANARIDevice device, ANARIFrame frame, const char* channel, uint32_t* width, uint32_t* height, ANARIDataType* pixelType);
     * }
     *
     * @param arena the arena to use for the returned MemorySegment (must stay alive until unmap is called)
     * @return mapped memory segment, valid until unmap(channel, arena) is called
     */
    public MemorySegment map(Arena arena, String channel, int width, int height, DataType pixelType) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return (MemorySegment) device.library.anari.mapFrame(
                    device.getPointer(),
                    getPointer(),
                    tempArena.allocateFrom(channel),
                    tempArena.allocateFrom(JAVA_INT, width),
                    tempArena.allocateFrom(JAVA_INT, height),
                    tempArena.allocateFrom(JAVA_INT, pixelType.code));
        } finally {
            device.checkForException();
        }
    }

    /**
     * {@snippet lang=c :
     * void anariUnmapFrame(ANARIDevice device, ANARIFrame frame, const char* channel);
     * }
     *
     * @param arena the arena used for the corresponding map() call (can be same or different)
     */
    public void unmap(Arena arena, String channel) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.unmapFrame(device.getPointer(), getPointer(), tempArena.allocateFrom(channel));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Convenience method that maps the frame using a confined arena.
     * The returned segment is only valid within the scope of the caller's try-with-resources block.
     * Prefer using {@link #map(Arena, String, int, int, DataType)} with a long-lived arena.
     *
     * @deprecated Use {@link #map(Arena, String, int, int, DataType)} instead
     */
    @Deprecated
    public MemorySegment map(String channel, int width, int height, DataType pixelType) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return (MemorySegment) device.library.anari.mapFrame(
                    device.getPointer(),
                    getPointer(),
                    tempArena.allocateFrom(channel),
                    tempArena.allocateFrom(JAVA_INT, width),
                    tempArena.allocateFrom(JAVA_INT, height),
                    tempArena.allocateFrom(JAVA_INT, pixelType.code));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Convenience method that unmaps the frame using a confined arena.
     * @deprecated Use {@link #unmap(Arena, String)} instead
     */
    @Deprecated
    public void unmap(String channel) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.unmapFrame(device.getPointer(), getPointer(), tempArena.allocateFrom(channel));
        } finally {
            device.checkForException();
        }
    }

}
