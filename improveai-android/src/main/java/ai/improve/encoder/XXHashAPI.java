package ai.improve.encoder;

import ai.improve.XXHashProvider;

public class XXHashAPI implements XXHashProvider {
    static {
        System.loadLibrary("xxhash");
    }

    public static native long  xxhash3(byte[] data, long seed);

    @Override
    public long xxhash(byte[] data, long seed) {
        return xxhash3(data, seed);
    }
}
