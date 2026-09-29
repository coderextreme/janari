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

/**
 * {@snippet lang=c :
 * typedef void* ANARIArray1D;
 * }
 *
 * @author Johann Sorel
 */
public final class Array1D extends Array<Array1D> {

    public Array1D(Device device, MemorySegment pointer, DataType type, long numElement0) {
        super(device, pointer, type, numElement0);
    }

    /**
     * Additional parameters accepted by ANARIArray1D with extension KHR_ARRAY1D_REGION.
     *
     * If region is not set, all elements are used as specified when the array was constructed.
     * When region is set, it is clamped to the range of the array’s constructed size respectively
     * and warnings should be emitted by the debug layer if region.upper is larger than capacity.
     *
     * The values specified by region are clamped such that the array will always be a valid range
     * containing at least one element. Thus region is clamped according to the following:
     * - region.upper is clamped from below by region.lower + 1
     * - region.upper is clamped from above by the array’s capacity
     * - region.lower is clamped from above by region.upper - 1
     */
    public Array1D setRegion(long start, long end) throws Throwable {
        try (Arena tempArena = Arena.ofConfined()) {
            set(Properties.Array1D.PARAM_REGION, DataType.UINT64_REGION1, tempArena.allocateFrom(ValueLayout.JAVA_LONG, start, end));
        }
        return this;
    }

}
