package ai.improve;

public class Utils {
    public static boolean isAndroid() {
        try {
            Class.forName("android.app.Activity");
            return true;
        } catch(ClassNotFoundException e) {
        }
        return false;
    }
}
