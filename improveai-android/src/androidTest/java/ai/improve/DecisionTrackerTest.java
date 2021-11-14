package ai.improve;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ai.improve.log.IMPLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class DecisionTrackerTest {
    private static final String Tag = "IMPDecisionTrackerTest";

    public static final String Track_URL = "https://d97zv0mo3g.execute-api.us-east-2.amazonaws.com/track";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.defaultTrackURL = Track_URL;
    }

    @Test
    public void testTracker() {
        DecisionTracker tracker = new DecisionTracker(Track_URL); // trackUrl is obtained from your Gym configuration
        DecisionModel model = new DecisionModel("theme");

        int fontSize = (Integer) model.chooseFrom(Arrays.asList(12, 16, 20)).get();
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
        DecisionTracker tracker = new DecisionTracker(Track_URL);
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
        decisionModel.chooseFrom(null).get();
    }

    /**
     * No assertion here.
     * Just a convenient place to observer log output of non-json-encodable objects tracking
     * */
    @Test
    public void testTrackNonJsonEncodable() {
        Map givens = new HashMap();
        givens.put("font", 12);
        givens.put("color", "#ffffff");
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).given(givens).get();
    }
}
