package com.sulin.codepose.event.framework.util;

public class AssetUtil {
    public static void isTrue(boolean isTrue, String message) {
        if (!isTrue) {
            throw new RuntimeException(message);
        }
    }
}
