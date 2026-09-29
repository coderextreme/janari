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
package org.codeberg.anari.javafx;

import org.codeberg.anari.api.Device;
import org.codeberg.anari.api.Frame;

/**
 *
 * @author Johann Sorel
 */
public abstract class AnariHandler {

    protected AnariPane pane;
    protected Device device;

    public void initialize(AnariPane pane) {
        this.pane = pane;
    }

    public void initialize(Device device) {
        this.device = device;
    }

    public void release(Device device) {
        this.device = null;
    }

    public void release(AnariPane pane) {
        this.pane = null;
    }

    public void requestRepaint() {
        pane.repaint();
    }

    public abstract Frame repaint(TimerState timer, int with, int height) throws Throwable;

    public abstract java.lang.foreign.Arena getArena();


}
