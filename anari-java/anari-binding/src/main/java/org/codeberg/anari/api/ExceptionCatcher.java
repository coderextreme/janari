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

import java.util.HashMap;
import java.util.Map;

/**
 * Catch severe error to raise java exceptions.
 *
 * @author Johann Sorel
 */
final class ExceptionCatcher extends StatusCallback {

    private final StatusCallback sub;
    private final Map<Long,AnariException> lastExceptions = new HashMap<>();

    public ExceptionCatcher(StatusCallback sub) {
        this.sub = sub;
    }

    @Override
    public synchronized void report(Device device, Object source, DataType sourceType, Severity severity, StatusCode code, String message) {

        if (Severity.FATAL_ERROR.equals(severity) || Severity.ERROR.equals(severity)) {
            if (DataType.LIBRARY.equals(sourceType)) {
                //it's okay to have an pointer address of 0 because the library may be null
            } else if (device.getPointer().address() == 0L) {
                throw new IllegalStateException("No object attached to ANARI status report");
            }
            lastExceptions.put(device.getPointer().address(), new AnariException(device, source, sourceType, severity, code, message));
        }

        if (sub != null) {
            sub.report(device, source, sourceType, severity, code, message);
        } else {
            AnariOO.LOGGER.log(severity.logLevel, "ANARI:" + severity +" " + code +" " +device + " " + source + " " + sourceType + "\n\t| " + message.replace("\n", "\n\t| "));
        }
    }

    public synchronized void checkException(long objectId) throws AnariException {
        final AnariException ex = lastExceptions.remove(objectId);
        if (ex != null) throw ex;
    }

}
