import org.apache.commons.math3.complex.Complex;
import org.testng.annotations.DataProvider;

/**
 * Created on 28.04.2017.
 */
public class TestData {

    public static Complex[] data = new Complex[]{
            new Complex(-0.02018756065260341, -0.014673108938647594), new Complex(0.936662672378036, -1.52169167390546),
    };

    @DataProvider(name = "data")
    public Object[][] f() {
        return new Object[][]{
                new Object[]{
                        null//data
                }
        };
    }
}