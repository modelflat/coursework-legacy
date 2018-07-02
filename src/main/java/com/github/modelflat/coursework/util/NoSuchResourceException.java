package com.github.modelflat.coursework.util;

/**
 * Created on 06.05.2017.
 */
public class NoSuchResourceException extends Throwable {

    NoSuchResourceException(String name) {
        super("No such resource: " + name);
    }

    NoSuchResourceException(Throwable e) {
        super(e);
    }
}
