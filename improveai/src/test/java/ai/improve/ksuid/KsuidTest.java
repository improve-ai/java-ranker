package ai.improve.ksuid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ai.improve.ksuid.KsuidGenerator.EPOCH;
import static ai.improve.ksuid.KsuidGenerator.PAYLOAD_BYTES;
import static ai.improve.ksuid.KsuidGenerator.UINT32_MAX;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import ai.improve.log.IMPLog;

public class KsuidTest {
    public static final String Tag = "KsuidTest";

    public static final String KSUID_ZERO = "000000000000000000000000000";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testKSUID() {
        KsuidGenerator ksuidGenerator = new KsuidGenerator();
        String ksuid = ksuidGenerator.next();
        assertNotNull(ksuid);
        assertEquals(ksuid.length(), 27);
    }

    @Test
    public void testMinTimestamp_minus_1() {
        long t = EPOCH - 1;
        byte[] payload = new byte[PAYLOAD_BYTES];
        KsuidGenerator ksuidGenerator = new KsuidGenerator();
        assertEquals("aWgEPLxxrZOFaOlDVFHTB3ZiQOO", ksuidGenerator.next(t, payload));
    }

    @Test
    public void testMinTimestamp() {
        long t = EPOCH;
        byte[] payload = new byte[PAYLOAD_BYTES];
        KsuidGenerator ksuidGenerator = new KsuidGenerator();
        assertEquals(KSUID_ZERO, ksuidGenerator.next(t, payload));
    }

    @Test
    public void testMinTimestamp_plus_1() {
        long t = EPOCH + 1;
        byte[] payload = new byte[PAYLOAD_BYTES];
        KsuidGenerator ksuidGenerator = new KsuidGenerator();
        assertEquals("000007n42DGM5Tflk9n8mt7Fhc8", ksuidGenerator.next(t, payload));
    }

    @Test
    public void testMaxTimestamp_minus_1() {
        long t = EPOCH + UINT32_MAX - 1;
        byte[] payload = new byte[PAYLOAD_BYTES];
        Arrays.fill(payload, (byte)255);
        KsuidGenerator ksuidGenerator = new KsuidGenerator();
        assertEquals("aWgEPLxxrZOFaOlDVFHTB3ZiQON", ksuidGenerator.next(t, payload));
    }

    @Test
    public void testMaxTimestamp() {
        long t = EPOCH + UINT32_MAX;
        byte[] payload = new byte[PAYLOAD_BYTES];
        Arrays.fill(payload, (byte)255);
        KsuidGenerator ksuidGenerator = new KsuidGenerator();
        assertEquals("aWgEPTl1tmebfsQzFP4bxwgy80V", ksuidGenerator.next(t, payload));
    }

    @Test
    public void testMaxTimestamp_plus_1() {
        long t = EPOCH + UINT32_MAX + 1;
        byte[] payload = new byte[PAYLOAD_BYTES];
        Arrays.fill(payload, (byte)255);
        KsuidGenerator ksuidGenerator = new KsuidGenerator();
        assertEquals("000007n42DGM5Tflk9n8mt7Fhc7", ksuidGenerator.next(t, payload));
    }
}
