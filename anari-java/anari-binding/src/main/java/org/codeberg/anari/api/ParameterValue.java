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
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * An immutable device-creation parameter, used with
 * {@link Library#newInitializedDevice(String, ParameterValue...) } to set
 * parameters which can only be assigned once, at device-creation time (see
 * the parameter info {@code initializer} flag returned by
 * {@link Device#getParameterInfo(DataType, String, String, DataType, String, DataType) }).
 *
 * {@snippet lang=c :
 * struct ANARIParameterValue {
 *   const char* name;
 *   ANARIDataType type;
 *   const void* value;
 * };
 * }
 *
 * @author Johann Sorel
 */
public final class ParameterValue {

    private static final ValueLayout.OfInt LAYOUT_TYPE = ValueLayout.JAVA_INT.withName("type");
    static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.ADDRESS.withName("name"),
        LAYOUT_TYPE,
        MemoryLayout.paddingLayout(4),
        ValueLayout.ADDRESS.withName("value")
    );

    public final String name;
    public final DataType type;
    public final MemorySegment value;

    /**
     * @param name parameter name
     * @param type parameter data type
     * @param value memory segment holding the raw parameter value, built the
     * same way as the {@code mem} argument of {@link Object#set(String, DataType, MemorySegment) }
     */
    public ParameterValue(String name, DataType type, MemorySegment value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /**
     * Write this list of initializers, followed by the required
     * {@code {NULL, UNKNOWN, NULL}} terminator, into a contiguous native array.
     *
     * @param arena arena used to allocate the array and the parameter names;
     * must outlive the native call using the returned array
     * @param initializers the initializer parameters
     * @return pointer to the first element of the native array
     */
    static MemorySegment toNativeArray(Arena arena, ParameterValue... initializers) {
        final MemorySegment array = arena.allocate(LAYOUT.byteSize() * (initializers.length + 1));
        for (int i = 0; i < initializers.length; i++) {
            final long offset = i * LAYOUT.byteSize();
            array.set(ValueLayout.ADDRESS, offset, arena.allocateFrom(initializers[i].name));
            array.set(LAYOUT_TYPE, offset + 8, initializers[i].type.code);
            array.set(ValueLayout.ADDRESS, offset + 16, initializers[i].value);
        }
        final long terminatorOffset = initializers.length * LAYOUT.byteSize();
        array.set(ValueLayout.ADDRESS, terminatorOffset, MemorySegment.NULL);
        array.set(LAYOUT_TYPE, terminatorOffset + 8, DataType.UNKNOWN.code);
        array.set(ValueLayout.ADDRESS, terminatorOffset + 16, MemorySegment.NULL);
        return array;
    }
}
