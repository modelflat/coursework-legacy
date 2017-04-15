package com.github.modelflat.coursework2;

import com.github.modelflat.coursework2.gl.EventListener;
import com.github.modelflat.coursework2.window.MainWindowEventHandler;
import com.jogamp.opengl.awt.GLCanvas;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;

import javax.swing.*;
//import java.awt;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Main {



    public static void main(String[] args) {

        for (String filename : new String[] {
                "D:\\Quadratic_Koch.1280.png",
                "D:\\Quadratic_Koch.1024.png",
                "D:\\Quadratic_Koch.800.png",
                "D:\\Quadratic_Koch.640.png",
                "D:\\Quadratic_Koch.320.png",
        }) {
            Image image;
            try (InputStream is = new FileInputStream(filename)) {
                image = new Image(is);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            BoxCountingCalculator calculator = new BoxCountingCalculator(image);
            System.out.println(
                    calculator.calculate((c) ->
                            !c.equals(Color.TRANSPARENT)
                                    //&& !((c.getRed() == 69) && (c.getGreen() == 69) && (c.getBlue() == 69))
                    ));

        }
        if (true) {
            return;
        }

        final JFrame frame = new JFrame("SDOSDOSD");

        GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(new EventListener());

        frame.add(canvas);
/*
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
        frame.setLocation(screenSize.width / 4, screenSize.height / 4);
        frame.addWindowListener(new MainWindowEventHandler(frame));*/

        frame.pack();

        frame.setVisible(true);
    }

}
