package ai.improve.android.hasher;

public class XXHashAPI {
    private static final String Tag = "XXFeatureEncoder";

    static {
        System.loadLibrary("xxhash");
    }

    public static native long  xxhash3(byte[] data, long seed);
}
