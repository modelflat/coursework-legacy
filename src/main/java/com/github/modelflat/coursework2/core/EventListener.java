package com.github.modelflat.coursework2.core;

import com.jogamp.opencl.*;
import com.jogamp.opencl.gl.CLGLContext;
import com.jogamp.opencl.gl.CLGLImage2d;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
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
public class EventListener implements GLEventListener {

    private static final String vertexShader =
            "#version 400\n" +
            "layout (location = 0) in vec2 position;\n" +
            "layout (location = 1) in vec2 vertexUV;\n" +
            "out vec2 fragmentUV;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(position, 1.0, 1.0);\n" +
            "    fragmentUV = vertexUV;\n" +
            "}";

    private static final String fragmentShader =
            "#version 400\n" +
            "in vec2 fragmentUV;\n" +
            "out vec4 out_color;\n" +
            "uniform sampler2D tex;\n" +
            "void main() {\n" +
            "    out_color = texture(tex, fragmentUV).rgba;\n" +
            "}";

    private static final String fragmentClearShader =
            "#version 400\n" +
                    "out vec4 out_color;\n" +
                    "void main() {\n" +
                    "    out_color = vec4(0.0, 0.0, 0.0, 0.0);\n" +
                    "}";

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

    private int imageWidth;
    private int imageHeight;
    private int clearProgram;
    private int textureFramebuffer;
    private IntBuffer textureDrawBuffers;

    public EventListener(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    private void initGLSide(GL4 gl, IntBuffer buffer) {
        // create texture
        texture = Util.createTexture(gl, buffer, imageWidth, imageHeight);


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

        clearProgram = Util.createProgram(gl, null, fragmentClearShader);
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
                    clContext.createProgram(is).build("-I ./src/main/resources/cl/include")
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
        IntBuffer buffer = GLBuffers.newDirectIntBuffer(imageWidth * imageHeight);
        initGLSide(gl, buffer);

        // interop
        imageCL = clContext.createFromGLTexture2d(
                buffer,
                texture.getTarget(), texture.getTextureObject(),
                0, CLMemory.Mem.WRITE_ONLY);

        // kernel
        newtonKernelWrapper.setBounds(-1.f, 1.f, -1.f, 1.f);
        newtonKernelWrapper.setC(cReal, cImag);
        newtonKernelWrapper.setT(t);
        newtonKernelWrapper.setRunParams(192, 2, 75, 3);
        newtonKernelWrapper.setImage(imageCL);

        clearKernel.setArg(0, imageCL);
        //clearKernel.rewind();

        drawable.setAutoSwapBufferMode(true);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("disposing...");
        clContext.release();
    }

    private float t = -1.0f;
    private float dt = .005f;
    private float cReal = .5f;
    private float dCReal = .005f;
    private float cImag = -.5f;
    private float dCImag = .005f;

    private boolean doEvolveOnT = true;
    private boolean doEvolveOnCReal = true;
    private boolean doEvolveOnCImag = true;
    private boolean doCLClear = true;
    private boolean doWaitForCL = true;

    private AtomicBoolean clearOnce = new AtomicBoolean(true);

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        // clear texture
        // FIXME not working properly; buffer seems shared with screen (?!)
//        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, textureFramebuffer);
//        {
//
//            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//            gl.glClear(GL4.GL_COLOR_BUFFER_BIT);
//        }
//        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, 0);

        queue.putAcquireGLObject(imageCL);

        if (doCLClear) {
            queue.put2DRangeKernel(clearKernel, 0, 0,
                    imageWidth, imageHeight, 0, 0);
        }

        newtonKernelWrapper.runOn(queue);

        queue.putReleaseGLObject(imageCL);

        if (doWaitForCL) {
            queue.finish();
        }

        if (doEvolveOnT) {
            t += dt;
            if (t < -1.0f || t > 1.0f) {
                dt = -dt;
            }
            newtonKernelWrapper.setT(t);
        }

        if (doEvolveOnCReal) {
            cReal += dCReal;
            if (cReal < -1.0f || cReal > 1.0f) {
                dCReal = -dCReal;
            }
            newtonKernelWrapper.setC(cReal, cImag);
        }

        if (doEvolveOnCImag) {
            cImag += dCImag;
            if (cImag < -1.0f || cImag > 1.0f) {
                dCImag = -dCImag;
            }
            newtonKernelWrapper.setC(cReal, cImag);
        }

        gl.glClear(GL_COLOR_BUFFER_BIT);
        gl.glUseProgram(program);
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

    public float getT() {
        return t;
    }

    public void setT(float t) {
        this.t = t;
    }

    public float getDt() {
        return dt;
    }

    public void setDt(float dt) {
        this.dt = dt;
    }

    public float getcReal() {
        return cReal;
    }

    public void setcReal(float cReal) {
        this.cReal = cReal;
    }

    public float getdCReal() {
        return dCReal;
    }

    public void setdCReal(float dCReal) {
        this.dCReal = dCReal;
    }

    public float getcImag() {
        return cImag;
    }

    public void setcImag(float cImag) {
        this.cImag = cImag;
    }

    public float getdCImag() {
        return dCImag;
    }

    public void setdCImag(float dCImag) {
        this.dCImag = dCImag;
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
}
