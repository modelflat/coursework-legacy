package com.github.modelflat.coursework2.core;

import com.github.modelflat.coursework2.BoxCountingCalculator;
import com.github.modelflat.coursework2.util.GLUtil;
import com.github.modelflat.coursework2.util.NoSuchResourceException;
import com.github.modelflat.coursework2.util.Util;
import com.jogamp.opencl.*;
import com.jogamp.opencl.gl.CLGLContext;
import com.jogamp.opencl.gl.CLGLImage2d;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.jogamp.opencl.CLProgram.define;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static java.lang.Math.log;
import static java.lang.System.nanoTime;

/**
 * Created on 18.03.2017.
 */
public class MyGLCanvasWrapper implements GLEventListener {

    private static final String vertexShader = "glsl/textureRender.vert";
    private static final String fragmentShader = "glsl/textureRender.frag";
    private static final String fragmentClearShader = "glsl/textureRenderStatisticalClear.frag";
    private static boolean profile = true;
    private static long MAX_TIME = 1_000_000_000 / 30; // 30 fps
    private float[] vertexData = new float[]{
            // texture vertex coords: x, y
            1.0f, 1.0f,
            1.0f, -1.0f,
            -1.0f, -1.0f,
            -1.0f, 1.0f,
            // texture UV coords (for texture mapping
            1.f, 1.f,
            1.f, 0.f,
            0.f, 0.f,
            0.f, 1.f,
    };
    private CLGLContext clContext;
    private CLCommandQueue queue;
    private NewtonKernelWrapper newtonKernelWrapper = new NewtonKernelWrapper();
    private CLKernel clearKernel;
    private CLGLImage2d<IntBuffer> imageCL;
    private int program;
    private Texture texture;
    private int vertexBufferObject;
    private int width;
    private int height;
    private int postClearProgram;
    private GLCanvas canvas;
    private boolean doEvolveBounds = false;
    private EvolvableParameter minX = new EvolvableParameter(false, -1, 0.0, -10.0, 0.0);
    private EvolvableParameter maxX = new EvolvableParameter(false, 1, -0.0, 0.0, 10.0);
    private EvolvableParameter minY = new EvolvableParameter(false, -1, 0.0, -10.0, 0.0);
    private EvolvableParameter maxY = new EvolvableParameter(false, 1, -0.0, -0.0, 10.0);
    private EvolvableParameter t = new EvolvableParameter(true,
            new ApproachingEvolutionStrategy(
                    ApproachingEvolutionStrategy.InternalStrategy.STOP_AT_POINT_OF_INTEREST, 0.0),
            3.4, -.01, -1.0, 10.0);
    private EvolvableParameter cReal = new EvolvableParameter(false, .5, -.05, -1.0, 1.0);
    private EvolvableParameter cImag = new EvolvableParameter(false, -.5, .05, -1.0, 1.0);
    private boolean doCLClear = true;
    private boolean doPostCLear = true;
    private boolean doWaitForCL = true;
    private boolean doEvolve = true;
    private boolean doRecomputeFractal = true;
    private boolean autoscale = false;
    private CLKernel boundingBoxKernel;
    private CLBuffer<IntBuffer> boundingBoxBuffer;
    private CLKernel computeKernel;
    private boolean doComputeD = false;
    private CLBuffer<IntBuffer> count;
    private PrintStream logFile;

    public MyGLCanvasWrapper(FPSAnimator animator, int width, int height, int canvasWidth, int canvasHeight) {
        this.width = width;
        this.height = height;

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.setSize(canvasWidth, canvasHeight);//width, height);
        animator.add(canvas);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // perform CL initialization
        GLContext context = drawable.getContext();
        try {
            initCLSide(context);
        } catch (NoSuchResourceException e) {
            e.printStackTrace();
        }

        // perform GL initialization
        GL4 gl = drawable.getGL().getGL4();
        IntBuffer buffer = GLBuffers.newDirectIntBuffer(width * height);
        try {
            initGLSide(gl, buffer);
        } catch (NoSuchResourceException e) {
            e.printStackTrace(); // TODO decide what to do
        }

        // interop
        imageCL = clContext.createFromGLTexture2d(
                buffer,
                texture.getTarget(), texture.getTextureObject(),
                0, CLMemory.Mem.WRITE_ONLY);

        // kernels
        newtonKernelWrapper.setBounds(minX.getValue(), maxX.getValue(), minY.getValue(), maxY.getValue());
        newtonKernelWrapper.setC(cReal.getValue(), cImag.getValue());
        newtonKernelWrapper.setT(t.getValue());
        newtonKernelWrapper.setImage(imageCL);

        clearKernel.setArg(0, imageCL);

        computeKernel.setArg(0, imageCL);

        boundingBoxKernel.setArg(0, imageCL);
        boundingBoxBuffer = clContext.createIntBuffer(4, CLMemory.Mem.WRITE_ONLY);
        boundingBoxKernel.setArg(1, boundingBoxBuffer);

        drawable.setAutoSwapBufferMode(true);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        clContext.release();
        if (logFile != null) {
            logFile.close();
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        long tt = 0, dd = 0, cd = 0, ev = 0, as = 0, pw = 0;
        if (doRecomputeFractal) {
            queue.putAcquireGLObject(imageCL);

            tt = nanoTime();
            if (doCLClear) {
                queue.put2DRangeKernel(clearKernel,
                        0, 0,
                        width, height, 0, 0)
                //.finish()
                ;
            }
            tt = (nanoTime() - tt);

            dd = nanoTime();
            newtonKernelWrapper.runOn(queue);
            queue.putReleaseGLObject(imageCL);
            dd = (nanoTime() - dd);

            pw = nanoTime();
            if (doWaitForCL) {
                queue.finish();
            }
            pw = nanoTime() - pw;

            cd = nanoTime();
            if (doComputeD) {
                // TODO move such things to UI
                System.out.println(t.getValue() + "\t" + computeD());
                if (logFile == null) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                    try {
                        logFile = new PrintStream("t-d@" + format.format(new Date()) + ".log");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                if (autoscale) { // TODO remove this dependency on autoscaling
                    logFile.println(t.getValue() + "\t" + computeD());
                }
            }
            cd = (nanoTime() - cd);

            if (doEvolve) {
                ev = nanoTime();
                evolve();
                ev = (nanoTime() - ev);

                as = nanoTime();
                if (autoscale) {
                    autoscale();
                }
                as = (nanoTime() - as);
            }
        }

        if (profile) {
            System.out.printf("wait: %d us\tclear: %d us\tdraw: %d us\td: %d us\tevolve: %d us\tautoscale: %d us\t" +
                            "remaining: %d us\n",
                    pw / 1000, tt / 1000, dd / 1000, cd / 1000, ev / 1000, as / 1000,
                    (MAX_TIME - (tt + dd + cd + ev + as + pw)) / 1000
            );
        }

        gl.glClear(GL_COLOR_BUFFER_BIT);
        if (doPostCLear) {
            gl.glUseProgram(postClearProgram);
        } else {
            gl.glUseProgram(program);
        }
        {
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferObject);
            {
                gl.glDrawArrays(GL4.GL_QUADS, 0, 4);
            }
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        drawable.getGL().getGL4().glViewport(x, y, width, height);
    }

    // ============================================================================
    // Private methods
    // ============================================================================

    private void initGLSide(GL4 gl, IntBuffer buffer) throws NoSuchResourceException {
        // create texture
        texture = GLUtil.createTexture(gl, buffer, width, height);

        postClearProgram = GLUtil.createProgram(gl, vertexShader, fragmentClearShader);
        // create program w/ 2 shaders
        program = GLUtil.createProgram(gl, vertexShader, fragmentShader);
        // get location of var "tex"
        int textureLocation = gl.glGetUniformLocation(program, "tex");
        gl.glUseProgram(program);
        {
            texture.enable(gl);
            texture.bind(gl);
            gl.glActiveTexture(GL4.GL_TEXTURE0);
            // tell opengl that a texture named "tex" points to GL_TEXTURE0
            gl.glUniform1i(textureLocation, 0);
        }

        // create VBO containing draw information
        vertexBufferObject = GLUtil.createVBO(gl, vertexData);
        // set buffer attribs
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferObject);
        {
            gl.glEnableVertexAttribArray(0);
            // tell opengl that first 16 values govern vertices positions (see vertexShader)
            gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0,
                    0);
            gl.glEnableVertexAttribArray(1);
            // tell opengl that remaining values govern fragment positions (see vertexShader)
            gl.glVertexAttribPointer(1, 2, GL4.GL_FLOAT, false, 0,
                    4 * 4 * 2);
        }
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

        // set clear color to dark red
        gl.glClearColor(0.2f, 0.0f, 0.0f, 1.0f);
    }

    private void initCLSide(GLContext context) throws NoSuchResourceException {
        CLPlatform chosenPlatform = CLPlatform.getDefault();
        CLDevice chosenDevice = GLUtil.findGLCompatibleDevice(chosenPlatform);

        if (chosenDevice == null) {
            throw new RuntimeException(String.format("no device supporting GL sharing on platform %s!",
                    chosenPlatform.toString()));
        }

        clContext = CLGLContext.create(context, chosenDevice);
        queue = chosenDevice.createCommandQueue();

        newtonKernelWrapper.initWith(
                clContext,
                clContext.createProgram(Util.loadSourceFile("cl/newton_fractal.cl"))
                        .build(
                                "-I ./src/main/resources/cl/include -cl-no-signed-zeros")
                        .createCLKernel("newton_fractal")
        );
        clearKernel = clContext.createProgram(Util.loadSourceFile("cl/clear_kernel.cl"))
                .build()
                .createCLKernel("clear");
        computeKernel = clContext.createProgram(Util.loadSourceFile("cl/compute_non_transparent.cl"))
                .build()
                .createCLKernel("compute");
        boundingBoxKernel = clContext.createProgram(Util.loadSourceFile("cl/compute_bounding_box.cl"))
                .build(define("WIDTH", width),
                        define("HEIGHT", height)).createCLKernel("compute_bounding_box");
    }

    private void evolve() {
        if (t.evolve()) {
            newtonKernelWrapper.setT(t.getValue());
        }

        if (cReal.evolve() | cImag.evolve()) {
            newtonKernelWrapper.setC(cReal.getValue(), cImag.getValue());
        }

        if (minY.evolve() | maxY.evolve() | maxX.evolve() | minX.evolve()) {
            newtonKernelWrapper.setBounds(minX.getValue(), maxX.getValue(), minY.getValue(), maxY.getValue());
        }
    }

    private void autoscale() {
        if (Math.abs(t.getValue()) < 1e-12) {
            System.out.println("autoscale stopped");
            autoscale = false;
        }
        // + 1) compute bounding box
        // - 2) save previous bounds to history (optional)
        // + 3) change bounds corresponding to computed box
        boundingBoxBuffer.getBuffer().put(0).put(width - 1).put(0).put(height - 1).rewind();
        queue.putWriteBuffer(boundingBoxBuffer, true)
                .put1DRangeKernel(boundingBoxKernel,
                        0, 4, 0)
                .finish()
                .putReadBuffer(boundingBoxBuffer, true);
        boxToBounds(boundingBoxBuffer.getBuffer());
    }

    private void boxToBounds(IntBuffer box) {
        double sX = maxX.getValue() - minX.getValue();
        double sY = maxY.getValue() - minY.getValue();

        int padding = 2;

        int dxMin = box.get(0) - padding;
        int dxMax = box.get(1) + padding;
        int dyMin = box.get(3) + padding;
        int dyMax = box.get(2) - padding;

        double newMinX = dxMin <= 0 ?
                minX.getValue() :
                minX.getValue() + ((double) dxMin / width) * sX;
        double newMaxX = dxMax >= width - 1 ?
                maxX.getValue() :
                maxX.getValue() - (1 - (double) dxMax / width) * sX;
        double newMinY = dyMin >= height - 1 ?
                minY.getValue() :
                minY.getValue() + (1 - (double) dyMin / height) * sY;
        double newMaxY = dyMax <= 0 ?
                maxY.getValue() :
                maxY.getValue() - ((double) dyMax / height) * sY;

        newtonKernelWrapper.setBounds(newMinX, newMaxX, newMinY, newMaxY);

        minX.setValue(newMinX);
        maxX.setValue(newMaxX);
        minY.setValue(newMinY);
        maxY.setValue(newMaxY);
    }

    private double computeD() {
        int startBoxSize = 5;
        int endBoxSize = width / 16;
        double[][] boxes = new double[2][endBoxSize - startBoxSize];
        // for all box sizes from 2 to maxXSize
        for (int k = startBoxSize, bI = 0; k < endBoxSize; k++, bI++) {
            int sizeY = (height / k) + (height % k == 0 ? 0 : 1);
            int sizeX = (width / k) + (width % k == 0 ? 0 : 1);
            int totalBoxes = sizeX * sizeY;
            int activeBoxes = computeActiveBoxes(k);
            //System.out.println(String.format("%d / %d", activeBoxes, totalBoxes));
            boxes[0][bI] = log(1.0 / k);
            boxes[1][bI] = log(activeBoxes);
            // System.out.println(String.format("[%d] (%d) %f %f", bI, k, Math.log(1.0 / k), Math.log(activeBoxes)));
        }
        return BoxCountingCalculator.normalEquations2d(boxes[0], boxes[1])[0];
    }

    private int computeActiveBoxes(int boxSize) {
        computeKernel.setArg(1, boxSize);
        if (count == null) {
            count = clContext.createIntBuffer(1, CLMemory.Mem.READ_WRITE);
        }
        count.getBuffer().put(0, 0);
        computeKernel.setArg(2, count);
        queue.putWriteBuffer(count, true)
                .put2DRangeKernel(
                        computeKernel, 0, 0, width, height, 0, 0);
        queue.finish().putReadBuffer(count, true);
        return count.getBuffer().get(0);
    }

    // ============================================================================
    // Getters/setters
    // ============================================================================

    public EvolvableParameter getMinX() {
        return minX;
    }

    public EvolvableParameter getMaxX() {
        return maxX;
    }

    public EvolvableParameter getMinY() {
        return minY;
    }

    public EvolvableParameter getMaxY() {
        return maxY;
    }

    public EvolvableParameter getT() {
        return t;
    }

    public EvolvableParameter getcReal() {
        return cReal;
    }

    public EvolvableParameter getcImag() {
        return cImag;
    }

    public boolean doCLClear() {
        return doCLClear;
    }

    public void setDoCLClear(boolean doCLClear) {
        this.doCLClear = doCLClear;
    }

    public boolean doPostCLear() {
        return doPostCLear;
    }

    public void setDoPostCLear(boolean doPostCLear) {
        this.doPostCLear = doPostCLear;
    }

    public boolean doWaitForCL() {
        return doWaitForCL;
    }

    public void setDoWaitForCL(boolean doWaitForCL) {
        this.doWaitForCL = doWaitForCL;
    }

    public boolean doEvolve() {
        return doEvolve;
    }

    public void setDoEvolve(boolean doEvolve) {
        this.doEvolve = doEvolve;
    }

    public boolean doEvolveBounds() {
        return doEvolveBounds;
    }

    public void setDoEvolveBounds(boolean doEvolveBounds) {
        this.doEvolveBounds = doEvolveBounds;
    }

    public boolean doRecomputeFractal() {
        return doRecomputeFractal;
    }

    public void setDoRecomputeFractal(boolean doRecomputeFractal) {
        this.doRecomputeFractal = doRecomputeFractal;
    }

    public GLCanvas getCanvas() {
        return canvas;
    }

    public boolean doComputeD() {
        return doComputeD;
    }

    public void setDoComputeD(boolean doComputeD) {
        this.doComputeD = doComputeD;
    }

    public NewtonKernelWrapper getNewtonKernelWrapper() {
        return newtonKernelWrapper;
    }
}
