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
import java.lang.foreign.ValueLayout;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import java.util.function.BiFunction;

/**
 * Base class of all ANARI objects. Objects are opaque, reference-counted
 * handles which accept named parameters and can publish properties. Parameter
 * changes only take effect once {@link #commit() } is called; uncommitted
 * parameters have no effect on rendering.
 *
 * {@snippet lang=c :
 * typedef void* ANARIObject;
 * }
 *
 * @author Johann Sorel
 * @param <T> object class
 */
public class Object<T extends Object <T>> {

    public static class SubType<T> {
        public final String name;
        private final BiFunction<Device, MemorySegment, T> create;

        public SubType(String name, BiFunction<Device, MemorySegment, T> create) {
            this.name = name;
            this.create = create;
        }

        public T create(Device device, MemorySegment segment) {
            return create.apply(device, segment);
        }
    }

    protected final Device device;
    protected final MemorySegment pointer;
    protected final DataType type;
    protected final String subtype;

    /**
     * Create a new device object.
     *
     * @param device device which created the object
     * @param pointer object pointer
     */
    public Object(Device device, DataType dataType, String subtype, MemorySegment pointer) {
        this.device = (device == null && this instanceof Device) ? (Device) this : device;
        this.type = dataType;
        this.subtype = subtype;
        this.pointer = pointer;
    }

    /**
     * Get object type.
     */
    public DataType getType() {
        return type;
    }

    /**
     * Get object subtype.
     * @return data type subtype, may be null if no subtype exist.
     */
    public String getSubtype() {
        return subtype;
    }

    /**
     * Get object pointer.
     *
     * @return object pointer
     */
    public MemorySegment getPointer() {
        return pointer;
    }

    /**
     * Get Object address as a MemorySegment.
     *
     * @param arena where to allocate memory
     * @return object address
     */
    public MemorySegment getAddress(Arena arena) {
        return arena.allocateFrom(ValueLayout.JAVA_LONG, pointer.address());
    }

    /**
     * Set a parameter with a raw native value, encoded according to dataType.
     * Most callers should prefer one of the typed {@code setXxx} overloads
     * instead of building the {@link MemorySegment} manually.
     *
     * {@snippet lang=c :
     * void anariSetParameter(ANARIDevice, ANARIObject, const char* parameterName, ANARIDataType, const void* value);
     * }
     */
    public T set(String name, DataType dataType, MemorySegment mem) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.setParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name), dataType.code, mem);
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Set a BOOL parameter.
     */
    public T setBool(String name, boolean value) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.setParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name), DataType.BOOL.code, tempArena.allocateFrom(JAVA_INT, value ? 1 : 0));
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Set a FLOAT32 parameter.
     */
    public T setFloat32(String name, float value) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.setParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name), DataType.FLOAT32.code, tempArena.allocateFrom(JAVA_FLOAT, value));
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Set a UINT64 parameter.
     */
    public T setUInt64(String name, long value) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.setParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name), DataType.UINT64.code, tempArena.allocateFrom(JAVA_LONG, value));
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Set a FLOAT32_VEC3 parameter.
     */
    public T setFloat32Vec3(String name, float ... values) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.setParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name), DataType.FLOAT32_VEC3.code, tempArena.allocateFrom(JAVA_FLOAT, values));
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Set a parameter to reference another ANARI object (e.g. an Array,
     * Geometry, Material, Sampler, Group, etc.), using the given object-handle
     * data type.
     */
    public T setObject(String name, DataType datatype, Object instance) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.setParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name), datatype.code,  instance.getAddress(tempArena));
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Set a STRING parameter.
     */
    public T setString(String name, String value) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.setParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name), DataType.STRING.code, tempArena.allocateFrom(value));
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Return the named parameter back to a state as if it had not been set.
     * Only applied once {@link #commit() } is called.
     */
    public void unset(String name) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            device.library.anari.unsetParameter(device.getPointer(), getPointer(), tempArena.allocateFrom(name));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Return this object back to a state as if no parameters had been set at
     * all. Only applied once {@link #commit() } is called.
     */
    public void unsetAll() throws AnariException, Throwable {
        try {
            device.library.anari.unsetAllParameters(device.getPointer(), getPointer());
        } finally {
            device.checkForException();
        }
    }

    /**
     * Apply all parameter changes made since the last commit (or since
     * creation) to this object.
     */
    public T commit() throws AnariException, Throwable {
        try {
            device.library.anari.commitParameters(device.getPointer(), getPointer());
        } finally {
            device.checkForException();
        }
        return (T) this;
    }

    /**
     * Directly map a device-managed 1D array on the given parameter for the
     * application to fill. Any previous array configuration on that parameter
     * is discarded. Must be unmapped with {@link #unmapParameterArray(Device, Object, MemorySegment) }
     * before the parameter can be altered through other means.
     */
    public MemorySegment mapParameterArray1D(String name, DataType datatype, long numElements1, long elementStride) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return (MemorySegment) device.library.anari.mapParameterArray1D(device.getPointer(), getPointer(), tempArena.allocateFrom(name), datatype.code, numElements1, tempArena.allocateFrom(ValueLayout.JAVA_LONG, elementStride));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Directly map a device-managed 2D array on the given parameter for the
     * application to fill. See {@link #mapParameterArray1D(String, DataType, long, long) }.
     */
    public MemorySegment mapParameterArray2D(String name, DataType datatype, long numElements1, long numElements2, long elementStride) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return (MemorySegment) device.library.anari.mapParameterArray2D(device.getPointer(), getPointer(), tempArena.allocateFrom(name), datatype.code, numElements1, numElements2, tempArena.allocateFrom(ValueLayout.JAVA_LONG, elementStride));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Directly map a device-managed 3D array on the given parameter for the
     * application to fill. See {@link #mapParameterArray1D(String, DataType, long, long) }.
     */
    public MemorySegment mapParameterArray3D(String name, DataType datatype, long numElements1, long numElements2, long numElements3, long elementStride) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return (MemorySegment) device.library.anari.mapParameterArray3D(device.getPointer(), getPointer(), tempArena.allocateFrom(name), datatype.code, numElements1, numElements2, numElements3, tempArena.allocateFrom(ValueLayout.JAVA_LONG, elementStride));
        } finally {
            device.checkForException();
        }
    }

    /**
     * Signal that the device is free to consume the data of a directly mapped
     * parameter array. Writing to the mapped pointer after this call is undefined behavior.
     */
    public void unmapParameterArray(Device device, Object object, MemorySegment name) throws AnariException, Throwable {
        device.library.anari.unmapParameterArray(device.getPointer(), object.getPointer(), name);
    }

    /**
     * Query a named property of this object. The waitMask indicates whether
     * the query should wait for the value to become available or return
     * instantly.
     *
     * @return 1 if the property was available and written to mem, 0 otherwise
     */
    public int getProperty(String name, DataType type, MemorySegment mem, long size, WaitMask mask) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            return (int) device.library.anari.getProperty(device.getPointer(), getPointer(), tempArena.allocateFrom(name), type.code, mem, size, mask.code);
        } finally {
            device.checkForException();
        }
    }

    /**
     * Query a named, mandatory property of this object, waiting for it to
     * become available.
     *
     * @throws IllegalStateException if the property could not be retrieved
     */
    public void getProperty(String name, DataType type, MemorySegment mem, long size) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final int res = (int) device.library.anari.getProperty(device.getPointer(), getPointer(), tempArena.allocateFrom(name), type.code, mem, size, WaitMask.WAIT.code);
            if (res != 1) throw new IllegalStateException("Expected response  1, but was " + res);
        } finally {
            device.checkForException();
        }
    }

    /**
     * Query the length (in bytes, including the terminating zero) of a STRING
     * property, by appending {@code .size} to the property name.
     */
    public long getPropertySize(String name) throws AnariException, Throwable {
        try (Arena tempArena = Arena.ofConfined()){
            final MemorySegment mem = tempArena.allocate(8);
            final int res = (int) device.library.anari.getProperty(device.getPointer(), getPointer(), tempArena.allocateFrom(name+".size"), DataType.UINT64.code, mem, 8, WaitMask.WAIT.code);
            if (res != 1) throw new IllegalStateException("Expected response  1, but was " + res);
            return mem.get(ValueLayout.JAVA_LONG, 0);
        } finally {
            device.checkForException();
        }
    }

    /**
     * Decrease the public reference count of this object. Once it reaches
     * zero, the object becomes inaccessible to host code. Releasing a NULL
     * handle is not an error.
     */
    public void release() throws AnariException, Throwable {
        try {
            device.library.anari.release(device.getPointer(), getPointer());
        } finally {
            device.checkForException();
        }
    }

    /**
     * Increase the public reference count of this object.
     */
    public void retain() throws AnariException, Throwable {
        try {
            device.library.anari.retain(device.getPointer(), getPointer());
        } finally {
            device.checkForException();
        }
    }

    @Override
    public String toString() {
        final long address = this.pointer.address();
        return this.getClass().getSimpleName() + ":" + ((address == 0L) ? "NULL" : (""+address));
    }

    @Override
    public int hashCode() {
        return (int) this.pointer.address();
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Object other = (Object) obj;
        return this.pointer.address() == other.pointer.address();
    }
}
