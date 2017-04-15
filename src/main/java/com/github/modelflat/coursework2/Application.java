package com.github.modelflat.coursework2;

import com.jogamp.opencl.*;
import com.jogamp.opencl.gl.CLGLContext;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Created on 18.03.2017.
 */
public class Application {

    private final int NUM_COMPONENTS = 4;

    private CLGLContext context;
    private CLCommandQueue queue;

    private GLContext glContext;

    private IntBuffer imagePixels;

    private CLImage2d<IntBuffer> imageCL;
    private int imageGL;

    public Application(GLContext glContext) {
        context = CLGLContext.create(glContext, CLPlatform.getDefault(), CLDevice.Type.GPU);
        queue = context.getMaxFlopsDevice().createCommandQueue();
    }

    public void f() {

    }

    public void init(GL4 gl, int width, int height) {
        imagePixels = ByteBuffer.allocateDirect(NUM_COMPONENTS * width * height).asIntBuffer();
        int[] store = new int[1];
        // create GL texture
        gl.glGenTextures(1, store, 0);
        imageGL = store[0];
        gl.glBindTexture(GL4.GL_TEXTURE_2D, imageGL);
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, NUM_COMPONENTS,
                640, 480, 0,
                GL4.GL_RGBA, GL4.GL_INT, imagePixels
        );
        //

        imageCL = context.createFromGLTexture2d(
                imagePixels, GL4.GL_TEXTURE_2D, imageGL, 0, CLMemory.Mem.READ_WRITE);
    }
}
