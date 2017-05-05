package com.github.modelflat.coursework2.window;

import com.github.modelflat.coursework2.core.MyGLCanvasWrapper;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Created on 18.03.2017.
 */
public class MainWindowEventHandler implements WindowListener {

    private Frame mainWindow;
    private MyGLCanvasWrapper wrapper;

    public MainWindowEventHandler(Frame mainWindow, MyGLCanvasWrapper wrapper) {
        this.mainWindow = mainWindow;
        this.wrapper = wrapper;
        wrapper.getAnimator().start();
    }

    @Override
    public void windowOpened(WindowEvent e) {
        System.out.println("window opened");
    }

    @Override
    public void windowClosing(WindowEvent e) {
        System.out.println("window closing...");
        mainWindow.dispose();
        wrapper.getAnimator().stop();
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
