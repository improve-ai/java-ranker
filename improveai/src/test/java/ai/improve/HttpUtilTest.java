package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import static ai.improve.DecisionTrackerTest.Track_URL;

import org.junit.jupiter.api.Test;

import ai.improve.log.IMPLog;
import ai.improve.util.HttpUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpUtilTest {
    public static final String Tag = "HttpUtilTest";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testSerializeBody_null_variant() throws MalformedURLException {
        DecisionTracker tracker = new DecisionTracker(new URL(Track_URL), null);

        Map<String, Object> body = new HashMap();
        body.put("variant", null);
        tracker.setBestVariant(null, body);
        assertEquals(HttpUtil.serializeBody(body), "{\"variant\":null}");
    }

    @Test
    public void testSerializeBody_null_leaf() throws MalformedURLException {
        DecisionTracker tracker = new DecisionTracker(new URL(Track_URL), null);

        Map<String, Object> body = new HashMap();

        Map<String, Object> variant = new HashMap<>();
        variant.put("theme", null);
        variant.put("font", null);
        variant.put("color", "#f0f0f0");

        tracker.setBestVariant(variant, body);

        assertEquals(HttpUtil.serializeBody(body), "{\"variant\":{\"color\":\"#f0f0f0\",\"theme\":null,\"font\":null}}");
    }

    @Test
    public void testIsJSONEncodable() {
        Map<String, Object> body = new HashMap<>();
        body.put("a", "aaaa");
        body.put("b", true);
        body.put("c", false);
        body.put("d", 0);
        body.put("e", 1.0f);
        body.put("f", 1/3.0);
        assertTrue(HttpUtil.isJsonEncodable(body));
    }

    @Test
    public void testIsJSONEncodable_NaN() {
        Map<String, Object> body = new HashMap<>();
        body.put("a", Double.NaN);
        assertFalse(HttpUtil.isJsonEncodable(body));
    }

    @Test
    public void testIsJsonEncodable_Date() {
        Map<String, Object> body = new HashMap<>();
        body.put("a", new Date());
        assertFalse(HttpUtil.isJsonEncodable(body));
    }

    @Test
    public void testIsJSONEncodable_Nested_Map() {
        Map<String, Object> body = new HashMap<>();
        body.put("a", "aaaa");
        body.put("b", true);
        body.put("c", false);
        body.put("d", 0);
        body.put("e", 1.0f);
        body.put("f", 1/3.0);
        assertTrue(HttpUtil.isJsonEncodable(body));

        Map<String, Object> child = new HashMap<>();
        child.put("a", "aaa");
        child.put("b", 0.01);

        body.put("child", child);
        assertTrue(HttpUtil.isJsonEncodable(body));

        child.put("c", new Date());
        body.put("child", child);
        assertFalse(HttpUtil.isJsonEncodable(body));
    }

    @Test
    public void testIsJSONEncodable_Nested_List() {
        Map<String, Object> body = new HashMap<>();
        body.put("a", "aaaa");
        body.put("b", true);
        body.put("c", false);
        body.put("d", 0);
        body.put("e", 1.0f);
        body.put("f", 1/3.0);
        assertTrue(HttpUtil.isJsonEncodable(body));

        List child = new ArrayList();
        child.add("aaa");
        child.add(0.01);

        body.put("child", child);
        assertTrue(HttpUtil.isJsonEncodable(body));

        child.add(new Date());
        body.put("child", child);
        assertFalse(HttpUtil.isJsonEncodable(body));
    }
}
