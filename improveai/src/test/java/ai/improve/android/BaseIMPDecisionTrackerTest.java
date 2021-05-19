package ai.improve.android;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class BaseIMPDecisionTrackerTest {
    public static final String Tag = "IMPDecisionModelTest";

    @Test
    public void testLoad() {
    }

    @Test
    public void testNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("variant", null);
        JSONObject root = new JSONObject(body);
//        IMPLog.d(Tag, "root=" + root.toString());
    }

}