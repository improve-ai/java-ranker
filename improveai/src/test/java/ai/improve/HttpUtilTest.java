package ai.improve;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ai.improve.log.IMPLog;
import ai.improve.util.HttpUtil;

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
