package com.github.modelflat.coursework2;

import com.github.modelflat.coursework2.core.MyGLCanvasWrapper;
import com.github.modelflat.coursework2.networking.Forismatic;
import com.github.modelflat.coursework2.ui.MainWindowEventHandler;
import com.github.modelflat.coursework2.util.NoSuchResourceException;
import com.github.modelflat.coursework2.util.Util;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import javax.swing.*;
import java.awt.*;

public class App {

    private static String defaultWindowName = "Newton Fractal";
    private static boolean enableCites = false;
    private static String citeLanguage = "en";

    private static App instance;

    private MyGLCanvasWrapper wrapper;

    private void initAndShowGUI() {
        JFrame frame = new JFrame(defaultWindowName);

        if (enableCites) {
            Forismatic.getQuote(citeLanguage, defaultWindowName).thenAccept(frame::setTitle);
        }

        frame.getContentPane().setLayout(
                new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS)
        );

        wrapper = new MyGLCanvasWrapper(800, 600);
        wrapper.getCanvas().addMouseWheelListener(event -> {
            int increment = event.getWheelRotation();
            wrapper.getMinX().incValue(increment);
            wrapper.getMaxX().incValue(-increment);
            wrapper.getMinY().incValue(increment);
            wrapper.getMaxY().incValue(-increment);
        });
        frame.add(wrapper.getCanvas());

        frame.addWindowListener(new MainWindowEventHandler(wrapper));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final JFXPanel fxPanel = new JFXPanel();
        fxPanel.setSize(400, 600);
        fxPanel.setPreferredSize(new Dimension(400, 600));
        frame.add(fxPanel);

        frame.pack();
        frame.setVisible(true);

        Platform.runLater(() -> initFX(fxPanel));
    }

    private void initFX(JFXPanel fxPanel) {
        Group root = new Group();
        try {
            root.getChildren().addAll((AnchorPane) Util.loadFXML("fxml/main_control_pane.fxml"));
        } catch (NoSuchResourceException e) {
            e.printStackTrace();
        }
        fxPanel.setScene(new Scene(root, Color.ALICEBLUE));
    }

    public MyGLCanvasWrapper getWrapper() {
        return wrapper;
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        instance = new App();
        SwingUtilities.invokeLater(() -> instance.initAndShowGUI());
    }

}
