package com.github.modelflat.coursework2.gl;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import java.awt.event.ComponentListener;
import java.nio.IntBuffer;

/**
 * Created on 18.03.2017.
 */
public class EventListener implements GLEventListener {

    int program;

    String[] vertexSrc = {
            ""
    };

    private int createShader(GL4 gl, int type, String[] src) {
        int vertexSh = gl.glCreateShader(type);
        IntBuffer buf = IntBuffer.allocate(src.length);
        for (String aVertexSrc : src) {
            buf.put(aVertexSrc.length());
        }
        buf.flip();
        gl.glShaderSource(vertexSh, src.length, src, buf);
        gl.glCompileShader(vertexSh);
        return vertexSh;
    }

    private int createShaderProgram(GL4 gl, String[] vertexSrc, String[] fragmentSrc, String[] computeSrc) {
        int prog = gl.glCreateProgram();

        int vert = createShader(gl, GL4.GL_VERTEX_SHADER, vertexSrc);
        gl.glAttachShader(prog, vert);
        gl.glDeleteShader(vert);
        int frag = createShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentSrc);
        gl.glAttachShader(prog, frag);
        gl.glDeleteShader(frag);
        int comp = createShader(gl, GL4.GL_COMPUTE_SHADER, computeSrc);
        gl.glAttachShader(prog, comp);
        gl.glDeleteShader(comp);

        gl.glLinkProgram(prog);
        return prog;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        //createShaderProgram(gl, vertexSrc, )
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        //gl.glUseProgram();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

    }
}
