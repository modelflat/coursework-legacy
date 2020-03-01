package com.github.modelflat.coursework;

import com.github.modelflat.coursework.core.MyGLCanvasWrapper;
import com.github.modelflat.coursework.networking.Forismatic;
import com.github.modelflat.coursework.ui.MainWindowEventHandler;
import com.github.modelflat.coursework.util.NoSuchResourceException;
import com.github.modelflat.coursework.util.Util;
import com.jogamp.opengl.util.FPSAnimator;
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

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        instance = new App();
        SwingUtilities.invokeLater(() -> instance.initAndShowGUI());
    }

    private void initAndShowGUI() {
        JFrame frame = new JFrame(defaultWindowName);

        if (enableCites) {
            Forismatic.getQuote(citeLanguage, defaultWindowName).thenAccept(frame::setTitle);
        }

        frame.setLayout(
                new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS)
        );

        FPSAnimator animator = new FPSAnimator(60, true);

        wrapper = new MyGLCanvasWrapper(animator,
                768, 768, 768, 768);
        wrapper.getCanvas().addMouseWheelListener(event -> {
            int increment = event.getWheelRotation();
            wrapper.getMinX().incValue(increment);
            wrapper.getMaxX().incValue(-increment);
            wrapper.getMinY().incValue(increment);
            wrapper.getMaxY().incValue(-increment);
        });
        frame.add(wrapper.getCanvas());

        frame.addWindowListener(new MainWindowEventHandler(animator));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final JFXPanel fxPanel = new JFXPanel();

        Platform.runLater(() -> {
            initFX(fxPanel);
            SwingUtilities.invokeLater(() -> {
                frame.add(fxPanel);
                frame.pack();
                frame.setVisible(true);
            });
        });
    }

    private void initFX(JFXPanel fxPanel) {
        Group root = new Group();
        AnchorPane pane;
        try {
            root.getChildren().addAll(pane = (AnchorPane) Util.loadFXML("fxml/main_control_pane.fxml"));
        } catch (NoSuchResourceException e) {
            e.printStackTrace();
            return;
        }
        fxPanel.setScene(new Scene(root, Color.ALICEBLUE));

        Dimension fixedSize = new Dimension();
        fixedSize.setSize(pane.getPrefWidth(), pane.getPrefHeight());

        fxPanel.setPreferredSize(fixedSize);
        fxPanel.setMinimumSize(fixedSize);
        fxPanel.setMaximumSize(fixedSize);
        fxPanel.setSize(fixedSize);
    }

    public MyGLCanvasWrapper getWrapper() {
        return wrapper;
    }

}
