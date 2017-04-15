import com.jogamp.opencl.*;
import org.apache.commons.math3.complex.Complex;
import org.testng.annotations.*;
import org.testng.internal.collections.Pair;

import java.io.*;
import java.nio.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * Created on 13.04.2017.
 */

public class TestOpenCLComplexArithmetic {

    private CLContext context;
    private CLCommandQueue queue;
    private CLDevice device;

    private static final int CL_PLATFORM_IDX = 1;

    private static float D;
    private static final int DATA_SIZE = 1024;

    @BeforeClass
    public void beforeClass() {
        CLPlatform platform = CLPlatform.listCLPlatforms()[CL_PLATFORM_IDX];
        System.out.println(platform);
        device = platform.getMaxFlopsDevice(CLDevice.Type.GPU);
        System.out.println(device);
        if (device.isDoubleFPAvailable()) {
            D = 1e-8f;
        } else {
            D = 1e-4f;
        }
    }

    @BeforeMethod
    public void setUpOpenCLEnvironment() {
        context = CLContext.create(device);
        queue = device.createCommandQueue();
    }

    @AfterMethod
    public void shutdownOpenCLEnvironment() {
        context.release();
    }

    private Complex[][] runProgram(String filename,
                                   int inElementCount, int outElementCount, long workSize)
            throws Exception {
        StringBuilder prefixSrc = new StringBuilder();
        try (BufferedReader is = new BufferedReader(
                new FileReader("./src/main/resources/complex.cl"))) {
            is.lines().forEach(s -> prefixSrc.append(s).append("\n"));
        }
        StringBuilder kernelSrc = new StringBuilder();
        try (BufferedReader is = new BufferedReader(
                new FileReader("./src/test/resources/" + filename))) {
            is.lines().forEach(s -> kernelSrc.append(s).append("\n"));
        }
        String source = prefixSrc.toString() + "\n" + kernelSrc.toString();

        int typeSize = device.isDoubleFPAvailable() ? 8 : 4;
        Class bufferType = device.isDoubleFPAvailable() ? DoubleBuffer.class : FloatBuffer.class;

        CLProgram program = context.createProgram(source).build();
        if (program.getBuildStatus(device).equals(CLProgram.Status.BUILD_ERROR)) {
            throw new Exception("build error");
        }
        CLKernel kernel = program.createCLKernel("f");

        ByteBuffer fbByte = ByteBuffer.allocateDirect(2 * inElementCount * typeSize)
                .order(device.isLittleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        Random random = new Random();
        for (int i = 0; i < 2 * inElementCount; i++) {
            if (device.isDoubleFPAvailable()) {
                fbByte.putDouble(random.nextDouble() * 10 - 5);
            } else {
                fbByte.putFloat(random.nextFloat() * 10 - 5);
            }
        }
        fbByte.flip();

        CLBuffer<ByteBuffer> in = context.createBuffer(fbByte, CLMemory.Mem.READ_ONLY, CLMemory.Mem.COPY_BUFFER);
        CLBuffer<ByteBuffer> out = device.isDoubleFPAvailable() ?
                context.createByteBuffer(2 * outElementCount * typeSize) :
                context.createByteBuffer( 2 * outElementCount * typeSize);

        kernel.setArg(0, in);
        kernel.setArg(1, out);

        queue   .put1DRangeKernel(kernel, 0, workSize, 0)
                .finish()
                .putReadBuffer(out, true);

        Complex[] src = new Complex[inElementCount];
        for (int i = 0; i < inElementCount; i++) {
            if (device.isDoubleFPAvailable()) {
                src[i] = new Complex(fbByte.getDouble(), fbByte.getDouble());
            } else {
                src[i] = new Complex(fbByte.getFloat(), fbByte.getFloat());
            }
        }

        Complex[] outC = new Complex[outElementCount];
        ByteBuffer outB = out.getBuffer();
        for (int i = 0; i < outElementCount; i++) {
            if (device.isDoubleFPAvailable()) {
                outC[i] = new Complex(outB.getDouble(), outB.getDouble());
            } else {
                outC[i] = new Complex(outB.getFloat(), outB.getFloat());
            }
        }

        Complex[][] result = new Complex[2][];
        result[0] = src;
        result[1] = outC;

        return result;
    }

    @Test
    public void testAddition() throws Exception {
        Complex[][] result = runProgram(
                "add_test.cl",
                DATA_SIZE * 2,
                DATA_SIZE,
                DATA_SIZE
        );
        for (int i = 0; i < DATA_SIZE; i++) {
            Complex r = result[0][2*i].add(result[0][2*i+1]);
            assertEquals(Complex.equals(r, result[1][i], D), true, r + " vs " + result[1][i]);
        }
    }

    @Test
    public void testDivision() throws Exception {
        Complex[][] result = runProgram(
                "div_test.cl",
                DATA_SIZE * 2,
                DATA_SIZE,
                DATA_SIZE
        );
        for (int i = 0; i < DATA_SIZE; i++) {
            Complex r = result[0][2*i].divide(result[0][2*i+1]);
            assertEquals(Complex.equals(r, result[1][i], D), true, r + " vs " + result[1][i]);
        }
    }

    @Test
    public void testSqrt() throws  Exception {
        Complex[][] result = runProgram(
                "sqrt_test.cl",
                DATA_SIZE,
                DATA_SIZE * 2,
                DATA_SIZE
        );
        for (int i = 0; i < DATA_SIZE; i++) {
            List<Complex> roots = result[0][i].nthRoot(2);
            assertEquals(Complex.equals(roots.get(0), result[1][2*i], D), true,
                    roots.get(0) + " vs " + result[1][2*i]);
            assertEquals(Complex.equals(roots.get(1), result[1][2*i + 1], D), true,
                    roots.get(1) + " vs " + result[1][2*i + 1]);
        }
    }

    @Test
    public void testCbrt() throws Exception {
        Complex[][] result = runProgram(
                "cbrt_test.cl",
                DATA_SIZE,
                DATA_SIZE * 3,
                DATA_SIZE
        );

        for (int i = 0; i < DATA_SIZE; i++) {
            List<Complex> roots = result[0][i].nthRoot(3);
            assertEquals(Complex.equals(roots.get(0), result[1][3*i], D), true,
                    roots.get(0) + " vs " + result[1][3*i]);
            assertEquals(Complex.equals(roots.get(1), result[1][3*i + 1], D), true,
                    roots.get(1) + " vs " + result[1][3*i + 1]);
            assertEquals(Complex.equals(roots.get(2), result[1][3*i + 2], D), true,
                    roots.get(2) + " vs " + result[1][3*i + 2]);
        }
    }

    @Test
    public void testSolveCubic() throws Exception {
        Complex[][] result = runProgram(
                "solve_cubic_test.cl",
                DATA_SIZE * 3,
                DATA_SIZE * 3,
                DATA_SIZE
        );

        Complex[] roots = new Complex[3];
        for (int i = 0; i < DATA_SIZE; i++) {
            new ComplexCubicEquation(Complex.ONE,
                    result[0][3*i], result[0][3*i + 1], result[0][3*i + 2]
            ).solve(roots);
            assertEquals(Complex.equals(roots[0], result[1][3*i], D), true,
                    roots[0] + " vs " + result[1][3*i]);
            assertEquals(Complex.equals(roots[1], result[1][3*i + 1], D), true,
                    roots[1] + " vs " + result[1][3*i + 1]);
            assertEquals(Complex.equals(roots[2], result[1][3*i + 2], D), true,
                    roots[2] + " vs " + result[1][3*i + 2]);
        }
    }

}
