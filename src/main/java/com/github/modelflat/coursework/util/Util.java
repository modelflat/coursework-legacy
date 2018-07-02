package com.github.modelflat.coursework.util;

import javafx.fxml.FXMLLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created on 06.05.2017.
 */
public class Util {

    public static void loadCustomControl(String name, Object controlObj) throws NoSuchResourceException {
        try {
            InputStream is = FXMLLoader.getDefaultClassLoader().getResourceAsStream(name);
            if (is == null) {
                throw new NoSuchResourceException(name);
            }
            FXMLLoader loader = new FXMLLoader();
            loader.setRoot(controlObj);
            loader.setController(controlObj);
            loader.load(is);
        } catch (IOException e) {
            throw new NoSuchResourceException(e);
        }
    }

    public static Object loadFXML(String name) throws NoSuchResourceException {
        try {
            InputStream is = FXMLLoader.getDefaultClassLoader().getResourceAsStream(name);
            if (is == null) {
                throw new NoSuchResourceException(name);
            }
            return new FXMLLoader().load(is);
        } catch (IOException e) {
            throw new NoSuchResourceException(e);
        }
    }

    public static String loadSourceFile(String name) throws NoSuchResourceException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                FXMLLoader.getDefaultClassLoader().getResourceAsStream(name)))) {
            br.lines().forEach((line) -> builder.append(line).append('\n'));
        } catch (IOException e) {
            throw new NoSuchResourceException(e);
        }
        return builder.toString();
    }

    public static String loadSourceWithIncludes(String name) throws NoSuchResourceException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                FXMLLoader.getDefaultClassLoader().getResourceAsStream(name)))) {
//            List<String> includes = br.lines().filter(line -> line.trim().startsWith("#include")).map(Util::getIncludeNameFromIncludeLine).collect(Collectors.toList());
//            for (String include : includes) {
//                // each line could throw an exception so do it in plain old foreach
//                builder.append("\n\n").append(loadSourceWithIncludes(include, prefix + "/" + "include")).append("\n\n");
//            }
            br.lines().forEach(builder::append);
        } catch (IOException e) {
            throw new NoSuchResourceException(e);
        }
        return builder.toString();
    }

    private static Pattern includePattern = Pattern.compile("#include \"([a-zA-Z0-9_/.]+)\"");

    private static String getIncludeNameFromIncludeLine(String line) {
        Matcher matcher = includePattern.matcher(line);
        if (!matcher.find()) {
            System.err.println(String.format("OpenCL include string \"%s\" is invalid!", line));
        }
        return matcher.group(1);
    }

}
