package com.revolut.backend.utils;

public class Args {

    private Args() {
    }

    public static void isTrue(boolean condition, String msg) {
        if (!condition) {
            throw new IllegalArgumentException(msg);
        }
    }

}
