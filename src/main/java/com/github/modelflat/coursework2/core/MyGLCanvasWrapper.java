package com.github.modelflat.coursework2.core;

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
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;

/**
 * Created on 18.03.2017.
 */
public class MyGLCanvasWrapper implements GLEventListener {

    private static final String vertexShader = "textureRender.vert";
    private static final String fragmentShader = "textureRender.frag";
    private static final String fragmentClearShader = "textureRenderStatisticalClear.frag";

    private float[] vertexData = new float[]{
            // texture vertex coords: x, y
            0.95f, 0.95f,
            0.95f, -0.95f,
            -0.95f, -0.95f,
            -0.95f, 0.95f,
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

    private void initGLSide(GL4 gl, IntBuffer buffer) {
        // create texture
        texture = Util.createTexture(gl, buffer, width, height);

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

        postClearProgram = Util.createProgram(gl, vertexShader, fragmentClearShader);
        // create program w/ 2 shaders
        program = Util.createProgram(gl, vertexShader, fragmentShader);
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
        vertexBufferObject = Util.createVBO(gl, vertexData);
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
        CLDevice chosenDevice = Util.findGLCompatibleDevice(chosenPlatform);
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
        initGLSide(gl, buffer);

        // interop
        imageCL = clContext.createFromGLTexture2d(
                buffer,
                texture.getTarget(), texture.getTextureObject(),
                0, CLMemory.Mem.WRITE_ONLY);

        // kernel
        newtonKernelWrapper.setBounds(-1.f, 1.f, -1.f, 1.f);
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

    private EvolvableParameter minX = new EvolvableParameter(-.001, 0.01, -1.0, 0.0);
    private boolean doEvolveOnMinX = false;
    private EvolvableParameter maxX = new EvolvableParameter(.001, -0.01, -0.0, 1.0);
    private boolean doEvolveOnMaxX = false;
    private EvolvableParameter minY = new EvolvableParameter(-.001, 0.01, -1.0, 0.0);
    private boolean doEvolveOnMinY = false;
    private EvolvableParameter maxY = new EvolvableParameter(.001, -0.01, -0.0, 1.0);
    private boolean doEvolveOnMaxY = false;

    private EvolvableParameter t = new EvolvableParameter(-1.0, .01, -1.0, 1.0,
            0.0, 1e-12);
    private boolean doEvolveOnT = true;
    private EvolvableParameter cReal = new EvolvableParameter(.5, -.05, -1.0);
    private boolean doEvolveOnCReal = false;
    private EvolvableParameter cImag = new EvolvableParameter(-.5, .05, -1.0);
    private boolean doEvolveOnCImag = false;

    private boolean doCLClear = true;
    private boolean doPostCLear = true;
    private boolean doWaitForCL = true;

    private boolean doEvolve = true;

    private AtomicBoolean clearOnce = new AtomicBoolean(true);

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        long timeNano = System.nanoTime();
        // clear texture
        // FIXME not working properly; buffer seems shared with screen (?!)
//        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, textureFramebuffer);
//        {
//            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//            gl.glClear(GL4.GL_COLOR_BUFFER_BIT);
//        }
//        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);

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
        if (doEvolveOnT) {
            t.evolve();
            System.out.println(t);
            newtonKernelWrapper.setT(t.getValue());
        }

        if (doEvolveOnCReal || doEvolveOnCImag) {
            if (doEvolveOnCReal) {
                cReal.evolve();
            }
            if (doEvolveOnCImag) {
                cImag.evolve();
            }
            newtonKernelWrapper.setC(cReal.getValue(), cImag.getValue());
        }


        if (doEvolveOnMinX || doEvolveOnMaxX || doEvolveOnMinY || doEvolveOnMaxY) {
            if (doEvolveOnMinX) {
                minX.evolve();
            }
            if (doEvolveOnMaxX) {
                maxX.evolve();
            }
            if (doEvolveOnMinY) {
                minY.evolve();
            }
            if (doEvolveOnMaxY) {
                maxY.evolve();
            }
            newtonKernelWrapper.setBounds(minX.getValue(), maxX.getValue(), minY.getValue(), maxY.getValue());
        }
    }

    public boolean isDoEvolveOnT() {
        return doEvolveOnT;
    }

    public void setDoEvolveOnT(boolean doEvolveOnT) {
        this.doEvolveOnT = doEvolveOnT;
    }

    public boolean isDoEvolveOnCReal() {
        return doEvolveOnCReal;
    }

    public void setDoEvolveOnCReal(boolean doEvolveOnCReal) {
        this.doEvolveOnCReal = doEvolveOnCReal;
    }

    public boolean isDoEvolveOnCImag() {
        return doEvolveOnCImag;
    }

    public void setDoEvolveOnCImag(boolean doEvolveOnCImag) {
        this.doEvolveOnCImag = doEvolveOnCImag;
    }

    public boolean isDoCLClear() {
        return doCLClear;
    }

    public void setDoCLClear(boolean doCLClear) {
        this.doCLClear = doCLClear;
    }

    public boolean isDoWaitForCL() {
        return doWaitForCL;
    }

    public void setDoWaitForCL(boolean doWaitForCL) {
        this.doWaitForCL = doWaitForCL;
    }

    public GLCanvas getCanvas() {
        return canvas;
    }
}
