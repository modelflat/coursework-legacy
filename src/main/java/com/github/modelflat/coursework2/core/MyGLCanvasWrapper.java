package com.github.modelflat.coursework2.core;

import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

/**
 * Created on 05.05.2017.
 */
public class MyGLCanvasWrapper {

    private GLCanvas canvas;
    private FPSAnimator animator;
    private EventListener eventListener;

    public MyGLCanvasWrapper(int width, int height) {
        canvas = new GLCanvas();

        eventListener = new EventListener(width, height);

        canvas.addGLEventListener(eventListener);
        canvas.setSize(width, height);

        animator = new FPSAnimator(60, true);
        animator.add(canvas);
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public EventListener getEventListener() {
        return eventListener;
    }

    public FPSAnimator getAnimator() {
        return animator;
    }
}
