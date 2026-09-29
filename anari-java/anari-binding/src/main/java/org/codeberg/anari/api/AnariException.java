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
 * Decorate an ANARI status callback as a Java exception.
 *
 * @author Johann Sorel
 */
public final class AnariException extends Exception {

    private final Device device;
    private final Object source;
    private final DataType sourceType;
    private final Severity severity;
    private final StatusCode code;
    private final String message;

    /**
     *
     * @param device device attached to the ANARI report.
     * @param source object attached to the ANARI report.
     * @param sourceType object type attached to the ANARI report.
     * @param severity ANARI report severity.
     * @param code ANARI report status code.
     * @param message  ANARI report message.
     */
    public AnariException(Device device, Object source, DataType sourceType, Severity severity, StatusCode code, String message) {
        this.device = device;
        this.source = source;
        this.sourceType = sourceType;
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    /**
     * @return device attached to the ANARI report.
     */
    public Device getDevice() {
        return device;
    }

    /**
     * @return object attached to the ANARI report.
     */
    public Object getSource() {
        return source;
    }

    /**
     * @return object type attached to the ANARI report.
     */
    public DataType getSourceType() {
        return sourceType;
    }

    /**
     * @return ANARI report severity.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @return ANARI report status code.
     */
    public StatusCode getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return severity +" " + code +" " +device + " " + source + " type:" + sourceType + " message:\n\t| " + message.replace("\n", "\n\t| ");
    }

}
