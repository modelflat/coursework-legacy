package com.github.modelflat.coursework2;

import com.github.modelflat.coursework2.core.MyGLCanvasWrapper;
import com.github.modelflat.coursework2.window.MainWindowEventHandler;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

//import java.awt;

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

        MyGLCanvasWrapper wrapper = new MyGLCanvasWrapper(800, 600);

        frame.add(wrapper.getCanvas());
        frame.setSize(800, 600);
        frame.addWindowListener(new MainWindowEventHandler(frame, wrapper));
        frame.pack();
        frame.setVisible(true);
    }

}
