package com.github.modelflat.coursework2;

import com.github.modelflat.coursework2.gl.EventListener;
import com.github.modelflat.coursework2.window.MainWindowEventHandler;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;

import javax.swing.*;
//import java.awt;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Main {



    public static void main(String[] args) {

        if (false) {
            for (String filename : new String[]{
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
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        final JFrame frame = new JFrame("eso");
        GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(new EventListener(
                3* screenSize.width / 4,
                3* screenSize.height/4));
        canvas.setSize(800, 600);

        frame.add(canvas);
        frame.setSize(800, 600);
        frame.addWindowListener(new MainWindowEventHandler(frame, canvas));
        frame.pack();
        frame.setVisible(true);
    }

}
