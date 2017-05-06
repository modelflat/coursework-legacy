package com.github.modelflat.coursework2.util;

import javafx.fxml.FXMLLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created on 06.05.2017.
 */
public class Util {

    public static Object loadFXML(String name) throws NoSuchResourceException {
        try {
            URL resourceUrl = FXMLLoader.getDefaultClassLoader().getResource(name);
            if (resourceUrl == null) {
                throw new NoSuchResourceException(name);
            }
            return FXMLLoader.load(resourceUrl);
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
