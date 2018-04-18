package com.github.modelflat.coursework2.core;

import com.jogamp.opencl.*;
import com.jogamp.opencl.gl.CLGLImage2d;
import com.jogamp.opengl.util.GLBuffers;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Created on 28.04.2017.
 */
public class NewtonKernelWrapper {

    private final int
            defaultIterCount = 512,
            defaultRunCount = 1,
            defaultWorkSize = 8192,
            defaultSkipCount = 128;
    private DoubleBuffer cBuffer = GLBuffers.newDirectDoubleBuffer(2);
    private CLBuffer<DoubleBuffer> cCLBuffer;
    private FloatBuffer colorBuffer = GLBuffers.newDirectFloatBuffer(4);
    private CLBuffer<FloatBuffer> colorCLBuffer;
    private CLKernel kernel;
    private CLContext context;
    private Random rng = new Random();
    private int workItems;

    public NewtonKernelWrapper() {
    }

    public void initWith(CLContext context, CLKernel kernel) {
        this.context = context;
        this.kernel = kernel;
        setRunParams(defaultWorkSize, defaultRunCount, defaultIterCount, defaultSkipCount);
        setColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    public void setBounds(double minX, double maxX, double minY, double maxY) {
        kernel.setArg(0, minX);
        kernel.setArg(1, maxX);
        kernel.setArg(2, minY);
        kernel.setArg(3, maxY);
    }

    public void setC(double real, double imaginary) {
        cBuffer.put(0, real);
        cBuffer.put(1, imaginary);
        if (cCLBuffer == null) {
            cCLBuffer = context.createBuffer(cBuffer, CLMemory.Mem.USE_BUFFER, CLMemory.Mem.READ_ONLY);
        }
        kernel.setArg(4, cCLBuffer);
    }

    public void setH(double h) {
        kernel.setArg(6, h);
    }

    public void setT(int t) { kernel.setArg(5, t); }

    public void setRunParams(int workItems, int runCount, int iterCount, int skipCount) {
        this.workItems = workItems;
        kernel.setArg(7, runCount);
        kernel.setArg(8, iterCount);
        kernel.setArg(9, skipCount);
    }

    public void setColor(float r, float g, float b, float a) {
        colorBuffer.put(0, r);
        colorBuffer.put(1, g);
        colorBuffer.put(2, b);
        colorBuffer.put(3, a);
        if (colorCLBuffer == null) {
            colorCLBuffer = context.createBuffer(colorBuffer, CLMemory.Mem.USE_BUFFER, CLMemory.Mem.READ_ONLY);
        }
        kernel.setArg(11, colorCLBuffer);
    }

    public void setImage(CLGLImage2d image) {
        kernel.setArg(12, image);
    }

    public CLCommandQueue runOn(CLCommandQueue queue) {
        kernel.setArg(10, rng.nextLong());
        return queue.put1DRangeKernel(kernel, 0, workItems, 0);
    }

    public int getDefaultIterCount() {
        return defaultIterCount;
    }

    public int getDefaultRunCount() {
        return defaultRunCount;
    }

    public int getDefaultWorkSize() {
        return defaultWorkSize;
    }

    public int getDefaultSkipCount() {
        return defaultSkipCount;
    }
}
