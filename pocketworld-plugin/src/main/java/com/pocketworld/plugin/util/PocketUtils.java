package com.pocketworld.plugin.util;

import java.util.regex.Pattern;

public final class PocketUtils {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    private PocketUtils() {}

    public static boolean isUUID(String uuid) {
        return UUID_PATTERN.matcher(uuid).matches();
    }
}
