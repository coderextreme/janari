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

/**
 * {@snippet lang=c :
 * typedef int ANARIStatusCode;
 * }
 *
 * @author Johann Sorel
 */
public enum StatusCode {
    /**
     * No error occurred.
     */
    NO_ERROR(0),
    /**
     * An error occurred whose cause does not fall into any of the other categories.
     */
    UNKNOWN_ERROR(1),
    /**
     * An invalid argument was passed, e.g. an invalid enum value or a NULL handle where not permitted.
     */
    INVALID_ARGUMENT(2),
    /**
     * The requested operation is invalid in the current state of the object or device.
     */
    INVALID_OPERATION(3),
    /**
     * The implementation ran out of memory to complete the operation.
     */
    OUT_OF_MEMORY(4),
    /**
     * The requested device subtype is not supported by this implementation.
     */
    UNSUPPORTED_DEVICE(5),
    /**
     * The application and implementation disagree on the targeted ANARI specification version.
     */
    VERSION_MISMATCH(6);
    public final int code;

    private StatusCode(int code) {
        this.code = code;
    }

    /**
     * Look up the enum constant matching the given native {@code ANARIStatusCode} value.
     */
    public static StatusCode forCode(int code) {
        for (StatusCode dt : StatusCode.values()) {
            if (dt.code == code) return dt;
        }
        throw new IllegalArgumentException("Unknown code " + code);
    }
}
