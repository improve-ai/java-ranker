package ai.improve.android;

import ai.improve.log.IMPLog;
import ai.improve.util.HttpUtil;
import ai.improve.util.TrackerHandler;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HttpUtilTest {
    public static final String Tag = "HttpUtilTest";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testSerializeBody_null_variant() {
        Map<String, Object> body = new HashMap();
        body.put("variant", null);
        TrackerHandler.setBestVariant(null, body);
        assertEquals(HttpUtil.serializeBody(body), "{\"variant\":null}");
    }

    @Test
    public void testSerializeBody_null_leaf() {
        Map<String, Object> body = new HashMap();

        Map<String, Object> variant = new HashMap<>();
        variant.put("theme", null);
        variant.put("font", null);
        variant.put("color", "#f0f0f0");

        TrackerHandler.setBestVariant(variant, body);

        assertEquals(HttpUtil.serializeBody(body), "{\"variant\":{\"color\":\"#f0f0f0\",\"theme\":null,\"font\":null}}");
    }
}
