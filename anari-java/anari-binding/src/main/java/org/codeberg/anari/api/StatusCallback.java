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
 * Errors and other messages (warnings, validation messages, debug information etc.)
 * from the device are reported via a status callback.
 *
 * {@snippet lang=c :
 * typedef void (*ANARIStatusCallback)(const void* userPtr, ANARIDevice device, ANARIObject source, ANARIDataType sourceType, ANARIStatusSeverity severity, ANARIStatusCode code, const char* message);
 * }
 *
 * @author Johann Sorel
 */
public abstract class StatusCallback {

    public final void call(MemorySegment userPtr, MemorySegment device, MemorySegment source, int sourceType, int severity, int code, MemorySegment message) {
        report(new Device(null, device),
               new Object(null, null, null, source),
               DataType.forCode(sourceType),
               Severity.forCode(severity),
               StatusCode.forCode(code),
               message.reinterpret(Integer.MAX_VALUE).getString(0));
    }

    public abstract void report(Device device, Object source, DataType sourceType, Severity severity, StatusCode code, String message);

}
