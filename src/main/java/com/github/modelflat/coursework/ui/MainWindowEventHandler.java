package com.github.modelflat.coursework.ui;

import com.jogamp.opengl.util.FPSAnimator;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Created on 18.03.2017.
 */
public class MainWindowEventHandler implements WindowListener {

    private FPSAnimator animator;

    public MainWindowEventHandler(FPSAnimator animator) {
        (this.animator = animator).start();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        animator.stop();
    }

    @Override
    public void windowClosed(WindowEvent e) {
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
