import com.jogamp.opencl.*;
import org.apache.commons.math3.complex.Complex;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;

/**
 * Created on 13.04.2017.
 */

public class TestOpenCLComplexArithmetic {

    private static final int DEFAULT_CL_PLATFORM_IDX = 0;
    private static final int DATA_SIZE = 10000;
    private static float D;
    private CLContext context;
    private CLCommandQueue queue;
    private CLDevice device;

    @BeforeClass
    public void beforeClass() {
        CLPlatform platform = CLPlatform.listCLPlatforms()[DEFAULT_CL_PLATFORM_IDX];
        System.out.println(platform);
        device = platform.getMaxFlopsDevice(CLDevice.Type.GPU);
        System.out.println(device);
        if (device.isDoubleFPAvailable()) {
            D = 1e-8f;
        } else {
            D = 1e-5f;
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

    private Complex[][] runProgram(String filename, ByteBuffer buffer,
                                   int inElementCount, int outElementCount, long workSize)
            throws Exception {

        String source = Util.readFilesAndConcat("./src/test/resources/cl/" + filename);

        int typeSize = device.isDoubleFPAvailable() ? 8 : 4;

        CLProgram program = context.createProgram(source).build("-I ./src/main/resources/cl/include");
        if (program.getBuildStatus(device).equals(CLProgram.Status.BUILD_ERROR)) {
            throw new Exception("build error");
        }
        CLKernel kernel = program.createCLKernel("f");

        CLBuffer<ByteBuffer> in = context.createBuffer(buffer, CLMemory.Mem.READ_ONLY, CLMemory.Mem.COPY_BUFFER);
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
                src[i] = new Complex(buffer.getDouble(), buffer.getDouble());
            } else {
                src[i] = new Complex(buffer.getFloat(), buffer.getFloat());
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

    private Complex[][] runProgram(String filename,
                                   int inElementCount, int outElementCount, long workSize)
            throws Exception {
        ByteBuffer buffer = allocateBuffer(inElementCount);
        Random random = new Random();
        for (int i = 0; i < 2 * inElementCount; i++) {
            if (device.isDoubleFPAvailable()) {
                buffer.putDouble(random.nextDouble() * 10 - 5);
            } else {
                buffer.putFloat(random.nextFloat() * 10 - 5);
            }
        }
        buffer.flip();
        return runProgram(filename, buffer, inElementCount, outElementCount, workSize);
    }

    private ByteBuffer allocateBuffer(int inElementCount) {
        return ByteBuffer.allocateDirect(2 * inElementCount * (device.isDoubleFPAvailable() ? 8 : 4))
                .order(device.isLittleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    private void assertComplexEquals(Complex a, Complex b) throws Exception {
        assertEquals(Complex.equals(a, b, D), true, a + " vs " + b);
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
        int h = 0;
        for (int i = 0; i < DATA_SIZE; i++) {
            new ComplexCubicEquation(Complex.ONE,
                    result[0][3*i], result[0][3*i + 1], result[0][3*i + 2]
            ).solve(roots);
            String forInput = String.format(" ||| for input: z^3 + %s*z^2 + %s*z + %s = 0",
                    result[0][3 * i],
                    result[0][3 * i + 1],
                    result[0][3 * i + 2]);
//            assertEquals(Complex.equals(roots[0], result[1][3*i], D), true,
//                    roots[0] + " vs " + result[1][3*i] + forInput);
//            assertEquals(Complex.equals(roots[1], result[1][3*i + 1], D), true,
//                    roots[1] + " vs " + result[1][3*i + 1] + forInput);
//            assertEquals(Complex.equals(roots[2], result[1][3*i + 2], D), true,
//                    roots[2] + " vs " + result[1][3*i + 2] + forInput);
            boolean passed = Complex.equals(roots[0], result[1][3 * i], D)
                    && Complex.equals(roots[1], result[1][3 * i + 1], D)
                    && Complex.equals(roots[2], result[1][3 * i + 2], D);
            if (!passed) {
                ++h;
            }
        }
        assertEquals(h, 0, String.format("%d/%d", h, DATA_SIZE));
    }

    @Test(dataProviderClass = TestData.class, dataProvider = "data")
    public void solveCubicOptimized(Complex[] input) throws Exception {
        Complex[][] result;
        int dsize;
        if (input != null) {
            int DATA_SIZE = input.length / 2;
            dsize = DATA_SIZE;
            ByteBuffer byteBuffer = allocateBuffer(DATA_SIZE * 2);
            for (Complex c : input) {
                byteBuffer.putDouble(c.getReal());
                byteBuffer.putDouble(c.getImaginary());
            }
            byteBuffer.flip();
            result = runProgram(
                    "solve_cubic_newton_fractal_optimized_test.cl",
                    byteBuffer,
                    DATA_SIZE * 2,
                    DATA_SIZE * 3,
                    DATA_SIZE
            );
        } else {
            dsize = DATA_SIZE;
            result = runProgram(
                    "solve_cubic_newton_fractal_optimized_test.cl",
                    DATA_SIZE * 2,
                    DATA_SIZE * 3,
                    DATA_SIZE
            );
        }

        Thread.sleep(500); // wait for ocl output

        Complex[] roots = new Complex[3];
        for (int i = 0; i < dsize; i++) {
            ComplexCubicEquation eq = new ComplexCubicEquation(
                    Complex.ONE,
                    result[0][2 * i],
                    Complex.ZERO,
                    result[0][2 * i + 1]
            );
            //eq.setPrecision(1e-7);
            boolean cannotBeSolved = false;
            try {
                eq.solve(roots);
            } catch (ComplexCubicEquation.SolutionException e) {
                cannotBeSolved = true;
                System.out.printf(eq + " cannot be solved with precision %f!", eq.getPrecision());
            }
            boolean passed = cannotBeSolved ||
                    Complex.equals(roots[0], result[1][3 * i], D)
                            && Complex.equals(roots[1], result[1][3 * i + 1], D)
                            && Complex.equals(roots[2], result[1][3 * i + 2], D);
            if (!passed) {
                System.out.println(String.format("new Complex%s, new Complex%s,", eq.getA(), eq.getC()));
                System.out.println("equation: " + eq);
                System.out.println("solution: " + Arrays.toString(roots));
                System.out.print("CL solution: " + result[1][3 * i] + " ");
                System.out.print(result[1][3 * i + 1] + " ");
                System.out.println(result[1][3 * i + 2]);
                System.out.println("===== STEPS =====");
                try {
                    eq.solve(roots, true);
                } catch (ComplexCubicEquation.SolutionException e) {
                    System.out.println("... and then a SolutionException occured :(");
                }
                System.out.println("=================");
            }
            if (!cannotBeSolved) {
                assertComplexEquals(roots[0], result[1][3 * i]);
                assertComplexEquals(roots[1], result[1][3 * i + 1]);
                assertComplexEquals(roots[2], result[1][3 * i + 2]);
            }
        }
    }

}
