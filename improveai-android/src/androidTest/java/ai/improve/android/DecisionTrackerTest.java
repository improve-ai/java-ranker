package ai.improve.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import ai.improve.DecisionModel;
import ai.improve.DecisionTracker;
import ai.improve.log.IMPLog;
import ai.improve.util.TrackerHandler;

import static ai.improve.android.DecisionModelTest.ModelURL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class DecisionTrackerTest {
    private static final String Tag = "IMPDecisionTrackerTest";

    public static final String Tracker_Url = "https://d97zv0mo3g.execute-api.us-east-2.amazonaws.com/track";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testHistoryId() {
        DecisionTracker tracker_0 = new DecisionTracker(Tracker_Url);

        String historyId_0 = TrackerHandler.getHistoryId();
        IMPLog.d(Tag, "testHistoryId, historyId=" + historyId_0);
        assertNotNull(historyId_0);

        DecisionTracker tracker_1 = new DecisionTracker(Tracker_Url);
        String historyId_1 = (String) TrackerHandler.getHistoryId();
        assertEquals(historyId_0, historyId_1);
    }

    @Test
    public void testTracker() throws Exception {
        DecisionTracker tracker = new DecisionTracker(Tracker_Url); // trackUrl is obtained from your Gym configuration

        URL modelUrl = new URL(ModelURL);
        int fontSize = (Integer) DecisionModel.load(modelUrl).track(tracker).chooseFrom(Arrays.asList(12, 16, 20)).get();
        IMPLog.d(Tag, "fontSize=" + fontSize);

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
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.track(new DecisionTracker(Tracker_Url));
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
        DecisionTracker tracker = new DecisionTracker(Tracker_Url);
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
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.track(new DecisionTracker(Tracker_Url));
        decisionModel.chooseFrom(null).get();
    }
}
