package ai.improve.android;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import ai.improve.IMPLog;
import ai.improve.IMPTrackerHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IMPDecisionTrackerTest {
    private static final String Tag = "IMPDecisionTrackerTest";

    private static final String Tracker_Url = "https://d97zv0mo3g.execute-api.us-east-2.amazonaws.com/track";

    static {
        IMPLog.setLogger(new IMPLoggerImp());
        IMPLog.enableLogging(true);
    }

    @Test
    public void testHistoryId() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionTracker tracker_0 = new IMPDecisionTracker(appContext, "");

        String historyId_0 = IMPTrackerHandler.getHistoryId();
        IMPLog.d(Tag, "testHistoryId, historyId=" + historyId_0);
        assertNotNull(historyId_0);

        IMPDecisionTracker tracker_1 = new IMPDecisionTracker(appContext, "");
        String historyId_1 = (String) IMPTrackerHandler.getHistoryId();
        assertEquals(historyId_0, historyId_1);
    }

    @Test
    public void testTracker() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionTracker tracker = new IMPDecisionTracker(appContext, "trackerUrl"); // trackUrl is obtained from your Gym configuration

        URL modelUrl = new URL("ModelURL");
        int fontSize = (Integer)IMPDecisionModel.load(modelUrl).track(tracker).chooseFrom(Arrays.asList(12, 16, 20)).get();

        tracker.trackEvent("Purchased", new HashMap<String, Object>(){
            {
                put("product_id", 8);
                put("value", 19.99);
            }
        });
    }

    /**
     * The tracking happens kind of like a side effect of get(), I'm not sure how
     * to unit test this.
     *
     * Currently, I'm just observing the log output of these never-would-fail
     * test case here.
     * */
    @Test
    public void testTrackerRequest() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionModel decisionModel = new IMPDecisionModel("music");
        decisionModel.track(new IMPDecisionTracker(appContext, Tracker_Url));
        decisionModel.chooseFrom(Arrays.asList("Hello", "Hi", "Hey")).get();
    }

    /**
     * The tracking happens kind of like a side effect of get(), I'm not sure how
     * to unit test this.
     *
     * Currently, I'm just observing the log output of these never-would-fail
     * test case here.
     * */
    @Test
    public void testTrackEvent() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionTracker tracker = new IMPDecisionTracker(appContext, Tracker_Url);
        tracker.trackEvent("hello");
        tracker.trackEvent("hello", new HashMap<String, Object>(){
            {
                put("price", 0.9);
                put("quantity", 10);
            }
        });
    }

    /**
     * The tracking happens kind of like a side effect of get(), I'm not sure how
     * to unit test this.
     *
     * Currently, I'm just observing the log output of these never-would-fail
     * test case here.
     * */
    @Test
    public void testTrackerNullVariants() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionModel decisionModel = new IMPDecisionModel("music");
        decisionModel.track(new IMPDecisionTracker(appContext, Tracker_Url));
        decisionModel.chooseFrom(null).get();
    }
}
