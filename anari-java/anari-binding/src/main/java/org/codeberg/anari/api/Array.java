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

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import org.codeberg.anari.Anari;

/**
 * {@snippet lang=c :
 * typedef void* ANARIArray;
 * }
 *
 * @author Johann Sorel
 * @param <T> array sub class
 */
public class Array<T extends Array<T>> extends Object<T> {

    protected final DataType type;
    protected final long[] size;
    protected final long nbElement;
    protected MemorySegment lastMapPointer;

    public Array(Device device, MemorySegment pointer, DataType type, long ... numElement) {
        super(device, type, null, pointer);
        this.type = type;
        this.size = numElement;

        long nb = numElement[0];
        for (int i = 1; i < numElement.length; i++) {
            nb *= numElement[i];
        }
        nbElement = nb;
    }

    /**
     * Set values in the array.
     *
     * @param autoMapUnMap true to automaticaly map/unmap array
     * @param datas to write in the array
     */
    public T set(boolean autoMapUnMap, byte ... datas) throws Throwable {
        if (lastMapPointer == null) map();
        lastMapPointer.asByteBuffer().put(0, datas);
        if (autoMapUnMap) unmap();
        return (T) this;
    }

    /**
     * Set values in the array.
     *
     * @param autoMapUnMap true to automaticaly map/unmap array
     * @param datas to write in the array
     */
    public T set(boolean autoMapUnMap, short ... datas) throws Throwable {
        if (lastMapPointer == null) map();
        lastMapPointer.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(0, datas);
        if (autoMapUnMap) unmap();
        return (T) this;
    }

    /**
     * Set values in the array.
     *
     * @param autoMapUnMap true to automaticaly map/unmap array
     * @param datas to write in the array
     */
    public T set(boolean autoMapUnMap, int ... datas) throws Throwable {
        if (lastMapPointer == null) map();
        lastMapPointer.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(0, datas);
        if (autoMapUnMap) unmap();
        return (T) this;
    }

    /**
     * Set values in the array.
     *
     * @param autoMapUnMap true to automaticaly map/unmap array
     * @param datas to write in the array
     */
    public T set(boolean autoMapUnMap, long ... datas) throws Throwable {
        if (lastMapPointer == null) map();
        lastMapPointer.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().put(0, datas);
        if (autoMapUnMap) unmap();
        return (T) this;
    }

    /**
     * Set values in the array.
     *
     * @param autoMapUnMap true to automaticaly map/unmap array
     * @param datas to write in the array
     */
    public T set(boolean autoMapUnMap, float ... datas) throws Throwable {
        if (lastMapPointer == null) map();
        lastMapPointer.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(0, datas);
        if (autoMapUnMap) unmap();
        return (T) this;
    }

    /**
     * Set values in the array.
     *
     * @param autoMapUnMap true to automaticaly map/unmap array
     * @param datas to write in the array
     */
    public T set(boolean autoMapUnMap, double ... datas) throws Throwable {
        if (lastMapPointer == null) map();
        lastMapPointer.asByteBuffer().asDoubleBuffer().put(0, datas);
        if (autoMapUnMap) unmap();
        return (T) this;
    }

    /**
     * {@link Anari#mapArray(MemorySegment, MemorySegment) }
     */
    public MemorySegment map() throws AnariException, Throwable {
        if (lastMapPointer != null) throw new IllegalArgumentException("Array already mapped");
        try {
            lastMapPointer = device.library.anari.mapArray(device.getPointer(), getPointer());
            lastMapPointer = lastMapPointer.reinterpret(type.byteSize * nbElement);
            return lastMapPointer;
        } finally {
            device.checkForException();
        }
    }

    /**
     * {@link Anari#unmapArray(MemorySegment, MemorySegment)  }
     */
    public void unmap() throws AnariException, Throwable {
        try {
            device.library.anari.unmapArray(device.getPointer(), getPointer());
        } finally {
            device.checkForException();
            lastMapPointer = null;
        }
    }

}
