package ai.improve.android.hasher;

import android.util.Log;

public class XXFeatureEncoder {
    private static final String Tag = "XXFeatureEncoder";

    static {
        System.loadLibrary("xxhash");
    }

    public native long  xxhash3(byte[] data, double seed);

    private int mSeed;

    public XXFeatureEncoder(int seed) {
        mSeed = seed;
        String s = "variant";
        byte[] bytes = s.getBytes();

        Log.d(Tag, "FeatureEncoder:  " + xxhash3(bytes, 3) + ", " + bytes.length + ", s =" + s);

    }
}
