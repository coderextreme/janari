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
import java.util.List;
import org.codeberg.anari.Anari;

/**
 * ANARI libraries are the mechanism that applications use to manage API device implementations.
 * Libraries are solely responsible for creating instances of devices.
 * Implementors may use a library to cache data or objects which are truly global
 * to their device implementations, such as contexts from underlying APIs.
 * Libraries are generally the first thing loaded by an application and the last
 * thing cleaned up. While libraries are represented by an opaque handle, they
 * are not considered an object per the given definition of an object and are
 * thus only usable in API calls which explicitly take ANARILibrary handles.
 *
 * {@snippet lang=c :
 * typedef void* ANARILibrary;
 * }
 *
 * @author Johann Sorel
 */
public final class Library {

    final AnariOO anaripoo;
    final Anari anari;
    public final MemorySegment pointer;
    public final ExceptionCatcher exceptionCatcher;

    public Library(AnariOO anaripoo, MemorySegment pointer, ExceptionCatcher exceptionCatcher) {
        this.anaripoo = anaripoo;
        this.anari = anaripoo.getAnari();
        this.pointer = pointer;
        this.exceptionCatcher = exceptionCatcher;
    }

    /**
     * Load a module implemented by this library.
     */
    public void loadModule(String name) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            anari.loadModule(pointer, tempArena.allocateFrom(name));
        } finally {
            exceptionCatcher.checkException(pointer.address());
        }
    }

    /**
     * Unload a module previously loaded with {@link #loadModule(String) }.
     */
    public void unloadModule(String name) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            anari.unloadModule(pointer, tempArena.allocateFrom(name));
        } finally {
            exceptionCatcher.checkException(pointer.address());
        }
    }

    /**
     * Create a new device of the given subtype. Devices are always usable with
     * sensible defaults; use {@link #newInitializedDevice(String, ParameterValue...) }
     * instead when immutable parameters must be set at creation time.
     */
    public Device newDevice(String name) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return new Device(this, (MemorySegment) anari.newDevice(pointer, tempArena.allocateFrom(name)));
        } finally {
            exceptionCatcher.checkException(pointer.address());
        }
    }

    /**
     * Create a new device of the given subtype, passing immutable initializer
     * parameters (e.g. choosing a particular GPU) which can only be assigned
     * once, at device-creation time.
     * <br>
     * New in ANARI 1.1.
     */
    public Device newInitializedDevice(String name, ParameterValue... initializers) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment nativeInitializers = ParameterValue.toNativeArray(tempArena, initializers);
            return new Device(this, (MemorySegment) anari.newInitializedDevice(pointer, tempArena.allocateFrom(name), nativeInitializers));
        } finally {
            exceptionCatcher.checkException(pointer.address());
        }
    }

    /**
     * List of device subtypes implemented in this library. The first (if any)
     * device is the default device.
     */
    public List<String> getDeviceSubtypes() throws AnariException, Throwable {
        try {
            final MemorySegment ptr = (MemorySegment) anari.getDeviceSubtypes(pointer);
            return AnariOO.fromNullTerminatedStrings(ptr);
        } finally {
            exceptionCatcher.checkException(pointer.address());
        }
    }

    /**
     * List of extensions implemented by the given device subtype (as returned
     * by {@link #getDeviceSubtypes() }).
     */
    public List<String> getDeviceExtensions(String deviceName) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment ptr = (MemorySegment) anari.getDeviceExtensions(pointer, tempArena.allocateFrom(deviceName));
            return AnariOO.fromNullTerminatedStrings(ptr);
        } finally {
            exceptionCatcher.checkException(pointer.address());
        }
    }

    /**
     * Unload this library. It is undefined behavior to unload a library while
     * instances of devices from it have not been released.
     */
    public void unload() throws AnariException, Throwable {
        try {
            anari.unloadLibrary(this.pointer);
        } finally {
            exceptionCatcher.checkException(pointer.address());
        }
    }

    @Override
    public String toString() {
        return "Library" + " " + this.pointer.address();
    }

}
