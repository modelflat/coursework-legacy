package com.github.modelflat.coursework2.core;

import com.jogamp.opencl.*;
import com.jogamp.opencl.gl.CLGLImage2d;
import com.jogamp.opengl.util.GLBuffers;

import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Created on 28.04.2017.
 */
public class NewtonKernelWrapper {

    private FloatBuffer cBuffer = GLBuffers.newDirectFloatBuffer(2);
    private CLBuffer<FloatBuffer> cCLBuffer;
    private CLKernel kernel;
    private CLContext context;
    private Random rng = new Random();
    private int workItems;

    public NewtonKernelWrapper(CLContext context, CLKernel kernel) {
        this.context = context;
        this.kernel = kernel;
    }

    public void setBounds(float minX, float maxX, float minY, float maxY) {
        kernel.setArg(0, minX);
        kernel.setArg(1, maxX);
        kernel.setArg(2, minY);
        kernel.setArg(3, maxY);
    }

    public void setC(float real, float imaginary) {
        cBuffer.put(0, real);
        cBuffer.put(1, imaginary);
        if (cCLBuffer == null) {
            cCLBuffer = context.createBuffer(cBuffer, CLMemory.Mem.USE_BUFFER, CLMemory.Mem.READ_ONLY);
        }
        kernel.setArg(4, cCLBuffer);
    }

    public void setT(float t) {
        kernel.setArg(5, t);
    }

    public void setRunParams(int workItems, int runCount, int iterCount, int skipCount) {
        this.workItems = workItems;
        kernel.setArg(6, runCount);
        kernel.setArg(7, iterCount);
        kernel.setArg(8, skipCount);
    }

    public void setImage(CLGLImage2d image) {
        kernel.setArg(10, image);
    }

    public CLCommandQueue runOn(CLCommandQueue queue) {
        kernel.setArg(9, rng.nextLong());
        return queue.put1DRangeKernel(kernel, 0, workItems, 0);
    }
}
