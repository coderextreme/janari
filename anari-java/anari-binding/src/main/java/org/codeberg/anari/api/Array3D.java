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

/**
 * {@snippet lang=c :
 * typedef void* ANARIArray3D;
 * }
 *
 * @author Johann Sorel
 */
public final class Array3D extends Array<Array3D> {

    public Array3D(Device device, MemorySegment pointer, DataType type, long numElement0, long numElement1, long numElement2) {
        super(device, pointer, type, numElement0, numElement1, numElement2);
    }

}
