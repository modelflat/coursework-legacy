import com.jogamp.opencl.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.FloatBuffer;
import java.util.Random;

import static org.testng.Assert.assertEquals;

/**
 * Created on 21.04.2017.
 */
public class TestOpenCLRandom {

    private static final int DEFAULT_CL_PLATFORM_IDX = 0;
    private static final int SAMPLES_COUNT = 1000;
    private static final int WORK_SIZE = 1000;
    private static final float ALLOWED_UNIFORM_DEVIATION = .05f;

    private CLContext context;
    private CLCommandQueue queue;
    private CLDevice device;

    @BeforeClass
    public void beforeClass() {
        CLPlatform platform = CLPlatform.listCLPlatforms()[DEFAULT_CL_PLATFORM_IDX];
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

    @Test
    public void testRandom() throws Exception {
        Random rng = new Random();
        String src = Util.readFilesAndConcat("./src/test/resources/cl/random_test.cl");
        CLProgram program = context.createProgram(src).build("-I ./src/main/resources/cl/include");
        if (program.getBuildStatus(device).equals(CLProgram.Status.BUILD_ERROR)) {
            System.out.println(program.getBuildLog());
            throw new RuntimeException("build error");
        }
        CLKernel kernel = program.createCLKernel("f");
        kernel.setArg(0, rng.nextLong());
        kernel.setArg(1, SAMPLES_COUNT);
        CLBuffer<FloatBuffer> out = context.createFloatBuffer(SAMPLES_COUNT * WORK_SIZE);
        kernel.setArg(2, out);

        queue.put1DRangeKernel(kernel, 0, WORK_SIZE, 0)
                .finish().putReadBuffer(out, true);

        FloatBuffer output = out.getBuffer();
        // basic statistic check column-wise
        float aveDiff = 0;
        for (int i = 0; i < WORK_SIZE; i++) {
            int gtOneHalf = 0;
            for (int j = 0; j < SAMPLES_COUNT; j++) {
                if (output.get(i * SAMPLES_COUNT + j) > 0.5f) {
                    gtOneHalf++;
                }
            }
            aveDiff += (float)gtOneHalf / (float)SAMPLES_COUNT;
        }
        float dev = (float) Math.abs(aveDiff / WORK_SIZE - .5);
        assertEquals(dev < ALLOWED_UNIFORM_DEVIATION, true,
                String.format("Distribution looks non-uniformly column-wise (deviation: %f / allowed %f)",
                        dev, ALLOWED_UNIFORM_DEVIATION));

        // basic statistic check row-wise
        aveDiff = 0;
        for (int i = 0; i < SAMPLES_COUNT; i++) {
            int gtOneHalf = 0;
            for (int j = 0; j < WORK_SIZE; j++) {
                if (output.get(i * WORK_SIZE + j) > 0.5f) {
                    gtOneHalf++;
                }
            }
            aveDiff += (float) gtOneHalf / (float) SAMPLES_COUNT;
        }
        dev = (float) Math.abs(aveDiff / WORK_SIZE - .5);
        assertEquals(dev < ALLOWED_UNIFORM_DEVIATION, true,
                String.format("Distribution looks non-uniformly row-wise (deviation: %f / allowed %f)",
                        dev, ALLOWED_UNIFORM_DEVIATION));
    }

}
