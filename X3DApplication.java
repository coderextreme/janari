// package org.codeberg.anari.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.codeberg.anari.javafx.AnariPane;
import org.codeberg.anari.javafx.AbstractHandler;

public class X3DApplication extends Application implements Runnable {

    public X3DApplication() {
    }

    @Override
    public void run() {
        launch();
    }

    public static void main(String[] args) {
        new Thread(new X3DApplication()).start();
    }

    @Override
    public void start(Stage stage) throws Exception {
        final AnariPane pane = new AnariPane();
try {
            // pane.setHandler(new X3DAnariAdvancedHandler4(new BoxEm().getRootNodeList().get(0)));
            pane.setHandler(new X3DAnariHandler(new BoxEm().getRootNodeList().get(0)));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        pane.setBackground(new Background(new BackgroundFill[0]));
        final Scene scene = new Scene(pane);
        scene.setFill(Color.TRANSPARENT);

        stage.setTitle("X3DJSAIL + ANARI Renderer");
        stage.initStyle(StageStyle.DECORATED); // Changed to decorated so you can move/close the window
        stage.setScene(scene);
        stage.setWidth(800);
        stage.setHeight(600);
        stage.show();
    }
}
