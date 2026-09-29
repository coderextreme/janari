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

import java.lang.foreign.AddressLayout;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * {@snippet lang=c :
 * typedef struct {
 *   const char* name;
 *   ANARIDataType type;
 *   } ANARIParameter;
 * }
 *
 * @author Johann Sorel
 */
public final class Parameter extends StructClass {

    private static final AddressLayout LAYOUT_NAME;
    private static final ValueLayout.OfInt LAYOUT_TYPE;
    static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        LAYOUT_NAME = ValueLayout.ADDRESS.withName("name"),
        LAYOUT_TYPE = ValueLayout.JAVA_INT.withName("type"),
        MemoryLayout.paddingLayout(4)
    ).withName("$anon$133:9");

    Parameter(MemorySegment struct) {
        super(struct);
    }

    @Override
    protected MemoryLayout getLayout() {
        return LAYOUT;
    }

    public String getName() {
        return AnariOO.toString(struct.get(LAYOUT_NAME, 0));
    }

    public DataType getType() {
        return DataType.forCode(struct.get(LAYOUT_TYPE, 8));
    }

    public static List<Parameter> toList(MemorySegment segment) {
        segment = segment.reinterpret(Long.MAX_VALUE);

        final List<Parameter> parameters = new ArrayList<>();
        for (int offset=0;;offset+=16) {
            long adr = segment.get(ValueLayout.JAVA_LONG, offset);
            if (adr == 0) break;
            MemorySegment sub = segment.asSlice(offset, LAYOUT.byteSize());
            Parameter param = new Parameter(sub);
            parameters.add(param);
        }

        return parameters;
    }
}
