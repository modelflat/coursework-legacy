package com.github.modelflat.coursework2.core;

import com.github.modelflat.coursework2.util.GLUtil;
import com.github.modelflat.coursework2.util.NoSuchResourceException;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.Arrays;

import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;

/**
 * Created on 18.03.2017.
 */
public class MyGLCanvasWrapper implements GLEventListener {

    private static final String vertexShader = "glsl/textureRender.vert";
    private static final String fragmentShader = "glsl/textureRender.frag";
    private static final String fragmentClearShader = "glsl/textureRenderStatisticalClear.frag";

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

    private NewtonKernelWrapper newtonKernelWrapper;

    private CLKernel clearKernel;
    private CLGLImage2d<IntBuffer> imageCL;

    private int program;
    private Texture texture;
    private int vertexBufferObject;

    private int width;
    private int height;

    private int postClearProgram;
    private int textureFramebuffer;
    private IntBuffer textureDrawBuffers;

    private GLCanvas canvas;
    private FPSAnimator animator;

    private boolean doEvolveBounds = false;

    private EvolvableParameter minX = new EvolvableParameter(false, -1, 0.01, -1.0, 0.0);
    private EvolvableParameter maxX = new EvolvableParameter(false, 1, -0.01, 0.0, 1.0);
    private EvolvableParameter minY = new EvolvableParameter(false, -1, 0.01, -1.0, 0.0);
    private EvolvableParameter maxY = new EvolvableParameter(false, 1, -0.01, -0.0, 1.0);

    private EvolvableParameter t = new EvolvableParameter(new ApproachingEvolutionStrategy(),
            -1.0, .02, -1.0, 1.0);
    private EvolvableParameter cReal = new EvolvableParameter(false, .5, -.05, -1.0, 1.0);
    private EvolvableParameter cImag = new EvolvableParameter(false, -.5, .05, -1.0, 1.0);

    private boolean doCLClear = true;
    private boolean doPostCLear = true;
    private boolean doWaitForCL = true;
    private boolean doEvolve = true;
    private boolean doRecomputeFractal = true;

    public MyGLCanvasWrapper(int width, int height) {
        this.width = width;
        this.height = height;

        canvas = new GLCanvas();

        canvas.addGLEventListener(this);
        canvas.setSize(width, height);

        animator = new FPSAnimator(60, true);
        animator.add(canvas);
    }

    public FPSAnimator getAnimator() {
        return animator;
    }

    private void initGLSide(GL4 gl, IntBuffer buffer) throws NoSuchResourceException {
        // create texture
        texture = GLUtil.createTexture(gl, buffer, width, height);

        IntBuffer out = IntBuffer.wrap(new int[]{0});
        gl.glGenFramebuffers(1, out);
        textureFramebuffer = out.get();
        textureDrawBuffers = IntBuffer.wrap(new int[]{GL4.GL_COLOR_ATTACHMENT0});
        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, textureFramebuffer);
        {
            gl.glFramebufferTexture(GL4.GL_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0,
                    texture.getTextureObject(), 0);
            gl.glDrawBuffers(1, textureDrawBuffers);
        }
        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);

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

    private void initCLSide(GLContext context) {
        CLPlatform chosenPlatform = CLPlatform.getDefault();
        System.out.println(chosenPlatform);
        CLDevice chosenDevice = GLUtil.findGLCompatibleDevice(chosenPlatform);
        if (chosenDevice == null) {
            throw new RuntimeException(String.format("no device supporting GL sharing on platform %s!",
                    chosenPlatform.toString()));
        }

        clContext = CLGLContext.create(context, chosenDevice);
        queue = chosenDevice.createCommandQueue();

        try (InputStream is = new FileInputStream("./src/main/resources/cl/newton_fractal.cl")) {
            newtonKernelWrapper = new NewtonKernelWrapper(
                    clContext,
                    clContext.createProgram(is).build("-I ./src/main/resources/cl/include -cl-no-signed-zeros")
                            .createCLKernel("newton_fractal"));
        } catch (IOException e) {
            e.printStackTrace(); //TODO
        }

        try (InputStream is = new FileInputStream("./src/main/resources/cl/clear_kernel.cl")) {
            clearKernel = clContext.createProgram(is).build().createCLKernel("clear");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // perform CL initialization
        GLContext context = drawable.getContext();

        initCLSide(context);

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

        // kernel
        newtonKernelWrapper.setBounds(minX.getValue(), maxX.getValue(), minY.getValue(), maxY.getValue());
        newtonKernelWrapper.setC(cReal.getValue(), cImag.getValue());
        newtonKernelWrapper.setT(t.getValue());
        newtonKernelWrapper.setRunParams(64, 4, 150, 3);
        newtonKernelWrapper.setImage(imageCL);

        clearKernel.setArg(0, imageCL);

        drawable.setAutoSwapBufferMode(true);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("disposing...");
        clContext.release();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        // clear texture
        // FIXME not working properly; buffer seems shared with screen (?!)
//        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, textureFramebuffer);
//        {
//            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//            gl.glClear(GL4.GL_COLOR_BUFFER_BIT);
//        }
//        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);

        if (doRecomputeFractal) {
            queue.putAcquireGLObject(imageCL);

            if (doCLClear) {
                queue.put2DRangeKernel(clearKernel, 0, 0,
                        width, height, 0, 0);
            }

            newtonKernelWrapper.runOn(queue);

            queue.putReleaseGLObject(imageCL);

            if (doWaitForCL) {
                queue.finish();
            }

            if (doEvolve) {
                evolve();
            }
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MyGLCanvasWrapper{");
        sb.append("vertexData=").append(Arrays.toString(vertexData));
        sb.append(", clContext=").append(clContext);
        sb.append(", queue=").append(queue);
        sb.append(", newtonKernelWrapper=").append(newtonKernelWrapper);
        sb.append(", clearKernel=").append(clearKernel);
        sb.append(", imageCL=").append(imageCL);
        sb.append(", program=").append(program);
        sb.append(", texture=").append(texture);
        sb.append(", vertexBufferObject=").append(vertexBufferObject);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", postClearProgram=").append(postClearProgram);
        sb.append(", textureFramebuffer=").append(textureFramebuffer);
        sb.append(", textureDrawBuffers=").append(textureDrawBuffers);
        sb.append(", canvas=").append(canvas);
        sb.append(", animator=").append(animator);
        sb.append(", doEvolveBounds=").append(doEvolveBounds);
        sb.append(", minX=").append(minX);
        sb.append(", maxX=").append(maxX);
        sb.append(", minY=").append(minY);
        sb.append(", maxY=").append(maxY);
        sb.append(", t=").append(t);
        sb.append(", cReal=").append(cReal);
        sb.append(", cImag=").append(cImag);
        sb.append(", doCLClear=").append(doCLClear);
        sb.append(", doPostCLear=").append(doPostCLear);
        sb.append(", doWaitForCL=").append(doWaitForCL);
        sb.append(", doEvolve=").append(doEvolve);
        sb.append(", doRecomputeFractal=").append(doRecomputeFractal);
        sb.append('}');
        return sb.toString();
    }
}
