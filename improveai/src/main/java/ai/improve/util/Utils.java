package ai.improve.util;

public class Utils {
    public static boolean isAndroid() {
        try {
            Class.forName("ai.improve.android.HistoryIdProviderImp");
            return true;
        } catch(ClassNotFoundException e) {
        }
        return false;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
