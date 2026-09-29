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
package org.codeberg.anari.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.codeberg.anari.javafx.AnariPane;

/**
 *
 * @author Johann Sorel
 */
public class JavaFxApplication extends Application implements Runnable {

    public JavaFxApplication(){
    }

    @Override
    public void run() {
        launch();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        final AnariPane pane = new AnariPane();
        try {
            pane.setHandler(new JavaFxHandler());
        } catch (Throwable ex) {
            ex.printStackTrace();
        }


        pane.setBackground(new Background(new BackgroundFill[0]));
        final Scene scene = new Scene(pane);
        scene.setFill(Color.TRANSPARENT);
        stage.setTitle("Examind Desktop");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(600);
        stage.show();

    }

}