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

import java.util.logging.Level;

/**
 * {@snippet lang=c :
 * typedef int ANARIStatusSeverity;
 * }
 *
 * @author Johann Sorel
 */
public enum Severity {
    /**
     * An unrecoverable error; the device and/or its objects are left in an undefined state.
     */
    FATAL_ERROR(1, Level.SEVERE),
    /**
     * A recoverable error; the specific call which triggered it failed, but the device remains usable.
     */
    ERROR(2, Level.SEVERE),
    /**
     * A validation warning, e.g. an unknown or unsupported parameter was set.
     */
    WARNING(3, Level.WARNING),
    /**
     * A hint that the current usage pattern may cause suboptimal performance.
     */
    PERFORMANCE_WARNING(4, Level.WARNING),
    /**
     * An informational message, not indicative of a problem.
     */
    INFO(5, Level.INFO),
    /**
     * A debug message, intended for implementors.
     */
    DEBUG(6, Level.FINE);

    public final Level logLevel;
    public final int code;

    private Severity(int code, Level level) {
        this.code = code;
        this.logLevel = level;
    }

    /**
     * Look up the enum constant matching the given native {@code ANARIStatusSeverity} value.
     */
    public static Severity forCode(int code) {
        for (Severity dt : Severity.values()) {
            if (dt.code == code) return dt;
        }
        throw new IllegalArgumentException("Unknown code " + code);
    }
}
