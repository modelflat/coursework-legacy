import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Created on 22.04.2017.
 */
public class Util {

    public static String readFilesAndConcat(String... filenames) throws IOException {
        StringBuilder src = new StringBuilder();
        for (String filename : filenames) {
            try (BufferedReader is = new BufferedReader(new FileReader(filename))) {
                is.lines().forEach(s -> src.append(s).append("\n"));
            }
        }
        return src.toString();
    }
}
