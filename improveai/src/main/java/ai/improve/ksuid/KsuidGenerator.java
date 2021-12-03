package ai.improve.ksuid;

import static ai.improve.ksuid.Base62.base62Encode;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

public class KsuidGenerator {
    public static final int EPOCH = 1400000000;

    public static final long UINT32_MAX = 4294967295L;

    public static final int PAYLOAD_BYTES = 16;

    private static final int TIMESTAMP_BYTES = 4;

    public static final int KSUID_STRING_LENGTH = 27;

    private static final int TOTAL_BYTES = TIMESTAMP_BYTES + PAYLOAD_BYTES;

    private SecureRandom random;

    public KsuidGenerator() {
        random = new SecureRandom();
    }

    public String next() {
        final byte[] payload = new byte[PAYLOAD_BYTES];
        random.nextBytes(payload);
        return next(System.currentTimeMillis()/1000, payload);
    }

    /**
     * @param t EPOCH time in seconds
     * */
    protected String next(long t, byte[] payload) {
        t -= EPOCH;

        if(t < 0 || t > UINT32_MAX) {
            return null;
        }

        if(payload.length != PAYLOAD_BYTES) {
            return null;
        }

        byte[] ksuidBytes = ByteBuffer.allocate(TOTAL_BYTES)
                .putInt((int)t)
                .put(payload)
                .array();

        return base62Encode(ksuidBytes, KSUID_STRING_LENGTH);
    }
}
