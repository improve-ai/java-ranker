package ai.improve.android;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class IMPDecisionTrackerTest {
    public static final String Tag = "IMPDecisionModelTest";

    @Test
    public void testLoad() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        IMPDecisionTracker tracker = new IMPDecisionTracker(appContext, "");
    }

    @Test
    public void testNull() {
        Map<String, Object> body = new HashMap<>();
        body.put("variant", null);
        JSONObject root = new JSONObject(body);
        IMPLog.d(Tag, "root=" + root.toString());
    }

}