import com.jogamp.opencl.*;
import org.apache.commons.math3.complex.Complex;
import org.testng.annotations.*;
import org.testng.internal.collections.Pair;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
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

    private static final float D = 1e-5f;
    private static final int DATA_SIZE = 1024;

    @BeforeClass
    public void beforeClass() {
        CLPlatform platform = CLPlatform.getDefault();
        System.out.println(platform);
        device = platform.getMaxFlopsDevice(CLDevice.Type.GPU);
        System.out.println(device);
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

    private String readCLTestFile(String name) throws IOException {
        StringBuilder prefix = new StringBuilder();
        try (BufferedReader is = new BufferedReader(
                new FileReader("./src/main/resources/complex.cl"))) {
            is.lines().forEach(s -> prefix.append(s).append("\n"));
        }
        StringBuilder kernel = new StringBuilder();
        try (BufferedReader is = new BufferedReader(
                new FileReader("./src/test/resources/" + name))) {
            is.lines().forEach(s -> kernel.append(s).append("\n"));
        }
        return prefix.toString() + "\n" + kernel.toString();
    }

    private CLBuffer<DoubleBuffer> makeRandomlyFilledDoubleComplexBuffer(int elementCount) {
        DoubleBuffer fb = ByteBuffer.allocateDirect(2 * elementCount * 8)
                .order(device.isLittleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN)
                .asDoubleBuffer();
        Random random = new Random();
        for (int i = 0; i < 2 * elementCount; i++) {
            fb.put(random.nextDouble()*10 - 5);
        }
        fb.flip();
        return context.createBuffer(fb, CLMemory.Mem.READ_ONLY, CLMemory.Mem.COPY_BUFFER);
    }

    private Complex[][] runProgram(String filename, int inElementCount, int outElementCount, long workSize)
            throws Exception {
        CLProgram program = context.createProgram(readCLTestFile(filename)).build();
        // System.out.println(program.getSource());
        if (program.getBuildStatus(device).equals(CLProgram.Status.BUILD_ERROR)) {
            throw new Exception("build error");
        }
        CLKernel kernel = program.createCLKernel("f");

        CLBuffer<DoubleBuffer> in = makeRandomlyFilledDoubleComplexBuffer(inElementCount);
        CLBuffer<DoubleBuffer> out = context.createDoubleBuffer(2 * outElementCount);

        kernel.setArg(0, in);
        kernel.setArg(1, out);

        queue   .put1DRangeKernel(kernel, 0, workSize, 0)
                .finish()
                .putReadBuffer(out, true);

        Complex[] src = new Complex[inElementCount];
        DoubleBuffer inB = in.getBuffer();
        for (int i = 0; i < inElementCount; i++) {
            src[i] = new Complex(inB.get(), inB.get());
        }

        Complex[] outC = new Complex[outElementCount];
        DoubleBuffer outB = out.getBuffer();
        for (int i = 0; i < outElementCount; i++) {
            outC[i] = new Complex(outB.get(), outB.get());
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
