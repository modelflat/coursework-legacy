package com.github.modelflat.coursework2.window;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Created on 18.03.2017.
 */
public class MainWindowEventHandler implements WindowListener {

    private Frame mainWindow;

    public MainWindowEventHandler(Frame mainWindow) {
        this.mainWindow = mainWindow;
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
