package ai.improve.util;

import java.net.MalformedURLException;
import java.net.URL;

public class Utils {
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isValidURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
        }
        return false;
    }
}
