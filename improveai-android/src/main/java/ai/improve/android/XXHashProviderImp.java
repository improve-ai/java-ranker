package ai.improve.android;

import ai.improve.android.hasher.XXHashAPI;

public class XXHashProviderImp extends XXHashProvider {
    @Override
    public long xxhash3(byte[] data, long seed) {
        return XXHashAPI.xxhash3(data, seed);
    }
}
