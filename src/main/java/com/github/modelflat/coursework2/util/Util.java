package com.github.modelflat.coursework2.util;

import javafx.fxml.FXMLLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

}
