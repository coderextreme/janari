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
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import static java.lang.foreign.ValueLayout.ADDRESS;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.codeberg.anari.Anari;

/**
 * Object oriented ANARI API.
 *
 * @author Johann Sorel
 */
public final class AnariOO {

    /**
     * The logger used by <abbr>ANARI</abbr> binding.
     *
     */
    public static final Logger LOGGER = Logger.getLogger("org.khronos.anari");

    private final Anari anari;
    private final Arena arena;

    public AnariOO(Anari anari, Arena arena) {
        this.anari = anari;
        this.arena = arena;
    }

    /**
     * Get raw ANARI API.
     * @return ANARI API
     */
    public Anari getAnari() {
        return anari;
    }

    /**
     * {@link Anari#loadLibrary(MemorySegment, MemorySegment, MemorySegment) }
     */
    public Library loadLibrary(String name, StatusCallback callback) throws AnariException, Throwable {
        final ExceptionCatcher catcher = new ExceptionCatcher(callback);
        try {
            return new Library(this, (MemorySegment) anari.loadLibrary(arena.allocateFrom(name), toNativeFunctionReference(catcher, arena), MemorySegment.NULL), catcher);
        } finally {
            catcher.checkException(0);
        }
    }

    public MemorySegment toNativeFunctionReference(StatusCallback callback, Arena arena) throws NoSuchMethodException, IllegalAccessException {
        final MethodHandle handle = MethodHandles.lookup().bind(callback, "call", anari.statusCallback.toMethodType());
        return Linker.nativeLinker().upcallStub(handle, anari.statusCallback, arena);
    }

    public MemorySegment toNativeFunctionReference(MemoryDeleter callback, Arena arena) throws NoSuchMethodException, IllegalAccessException {
        final MethodHandle handle = MethodHandles.lookup().bind(callback, "release", anari.memoryDeleter.toMethodType());
        return Linker.nativeLinker().upcallStub(handle, anari.memoryDeleter, arena);
    }

    public MemorySegment toNativeFunctionReference(FrameCompletionCallback callback, Arena arena) throws NoSuchMethodException, IllegalAccessException {
        final MethodHandle handle = MethodHandles.lookup().bind(callback, "call", anari.frameCompletionCallback.toMethodType());
        return Linker.nativeLinker().upcallStub(handle, anari.frameCompletionCallback, arena);
    }

    /**
     * Returns a {@code NULL}-terminated array as a modifiable list of strings.
     * This way to encode arrays of strings is specific to <abbr>ANARI</abbr>.
     *
     * @param  result  the result of a native method call, or {@code null}.
     * @return the results as strings, or {@code null} if the result was null.
     */
    @SuppressWarnings("restricted")
    public static List<String> fromNullTerminatedStrings(MemorySegment result) {
        if (isNull(result)) {
            return null;
        }
        result = result.reinterpret(Integer.MAX_VALUE);
        final var  items  = new ArrayList<String>();
        final long stride = ADDRESS.byteSize();
        String item;
        for (long offset = 0; (item = toString(result.get(ADDRESS, offset))) != null; offset += stride) {
            items.add(item);
        }
        return items;
    }

    /**
     * Returns whether the given result is null.
     *
     * @param  result  the result of a native method call, or {@code null}.
     * @return whether the given result is null or a C/C++ {@code NULL}.
     */
    public static boolean isNull(final MemorySegment result) {
        return (result == null) || result.address() == 0;
    }

    /**
     * Returns the value of a native function returning a null-terminated {@code char*}.
     * The string is assumed encoded in UTF-8.
     *
     * @param  result  the result of a native method call, or {@code null}.
     * @return the result as a string, or {@code null} if the result was null.
     */
    @SuppressWarnings("restricted")
    public static String toString(final MemorySegment result) {
        return isNull(result) ? null : result.reinterpret(Integer.MAX_VALUE).getString(0);
    }
}
