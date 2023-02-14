package me.modify.pocketworld.util;

import java.util.regex.Pattern;

public class PocketUtils {

    public static boolean isUUID(String uuid) {
        String regex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(uuid).matches();
    }

}
