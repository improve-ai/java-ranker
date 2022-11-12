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
        // Allocate an extra zero value byte in the first position, so that the unsigned 32bit UTC
        // timestamp value in range (INT32_MAX, UINT32_MAX] is not treated as negative value and thus being
        // encoded as "000000000000000000000000000" by the base62 encoder
        byte[] ksuidBytes = ByteBuffer.allocate(TOTAL_BYTES+1)
                .put((byte)0)
                .putInt((int)(t - EPOCH))
                .put(payload)
                .array();
        return base62Encode(ksuidBytes, KSUID_STRING_LENGTH);
    }
}
