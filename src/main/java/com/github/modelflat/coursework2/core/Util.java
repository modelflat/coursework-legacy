package com.github.modelflat.coursework2.core;

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created on 27.04.2017.
 */
public class Util {

    public static <T extends GL3> int createVBO(T gl, float[] vertexData) {
        int[] result = new int[1];
        gl.glGenBuffers(1, IntBuffer.wrap(result));
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, result[0]);

        FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(vertexData);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexData.length * 4, buffer, GL4.GL_STATIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        return result[0];
    }

    public static <T extends GL3> Texture createTexture(T gl, IntBuffer buffer, int width, int height) {
        Texture texture;
        TextureData textureData = new TextureData(
                gl.getGLProfile(),
                GL4.GL_RGBA, width, height, 0, GL4.GL_RGBA, GL4.GL_UNSIGNED_INT_8_8_8_8_REV,
                false, false, false, buffer, null
        );
        texture = TextureIO.newTexture(textureData);
        texture.setTexParameteri(gl, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
        texture.setTexParameteri(gl, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        texture.setTexParameteri(gl, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        texture.setTexParameteri(gl, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
        return texture;
    }

    public static <T extends GL3> Texture createTexture(T gl, String filename, int imageWidth, int imageHeight)
            throws IOException {
        Texture texture;
        String[] splitted = filename.split("\\.");
        try (InputStream is = new FileInputStream(filename)) {
            texture = TextureIO.newTexture(is, false,
                    "." + splitted[splitted.length - 1].toLowerCase());

            texture.setTexParameteri(gl, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
            texture.setTexParameteri(gl, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
            texture.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
            texture.setTexParameteri(gl, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        }
        return texture;
    }

    public static <T extends GL3> int createShader(T gl, int type, String[] src) {
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

    public static <T extends GL3> int createProgram(T gl,
                                                    String[] vertexShader,
                                                    String[] fragmentShader) {
        int prog = gl.glCreateProgram();

        if (vertexShader != null) {
            int vert = createShader(gl, GL4.GL_VERTEX_SHADER, vertexShader);
            gl.glAttachShader(prog, vert);
            gl.glDeleteShader(vert);
        }

        if (fragmentShader != null) {
            int frag = createShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentShader);
            gl.glAttachShader(prog, frag);
            IntBuffer logSize = IntBuffer.wrap(new int[1]);
            gl.glGetShaderiv(frag, GL4.GL_INFO_LOG_LENGTH, logSize);
            ByteBuffer buf = ByteBuffer.allocate(logSize.get(0) + 1);
            gl.glGetShaderInfoLog(frag, logSize.get(0), logSize, buf);
            System.out.println(new String(buf.array()));
            gl.glDeleteShader(frag);
        }

        gl.glLinkProgram(prog);
        return prog;
    }

    public static <T extends GL3> int createProgram(T gl, String vertexShaderFile, String fragmentShaderFile) {
        StringBuilder vertexShader = new StringBuilder();
        String basePath = "./src/main/resources/glsl/";
        try (BufferedReader br = new BufferedReader(new FileReader(basePath + vertexShaderFile))) {
            br.lines().forEach((line) -> vertexShader.append(line).append('\n'));
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder fragmentShader = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(basePath + fragmentShaderFile))) {
            br.lines().forEach((line) -> fragmentShader.append(line).append('\n'));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return createProgram(gl, new String[]{vertexShader.toString()}, new String[]{fragmentShader.toString()});
    }

    public static CLDevice findGLCompatibleDevice(CLPlatform platform) {
        for (CLDevice device : platform.listCLDevices(CLDevice.Type.GPU)) {
            if (device.isGLMemorySharingSupported()) {
                return device;
            }
        }
        return null;
    }

}
