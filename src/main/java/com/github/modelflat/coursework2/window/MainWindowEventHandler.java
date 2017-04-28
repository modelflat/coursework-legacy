package com.github.modelflat.coursework2.window;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Created on 18.03.2017.
 */
public class MainWindowEventHandler implements WindowListener {

    private final Animator animator;
    private Frame mainWindow;
    private GLCanvas canvas;

    public MainWindowEventHandler(Frame mainWindow, GLCanvas canvas) {
        this.mainWindow = mainWindow;
        this.canvas = canvas;

        animator = new Animator();
        animator.add(canvas);
        animator.start();
    }

    @Override
    public void windowOpened(WindowEvent e) {
        System.out.println("window opened");
    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.out.println("window closing...");
        //
        mainWindow.dispose();
        animator.stop();
    }

    @Override
    public void windowClosed(WindowEvent e) {

        System.out.println("window closed.");
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}
