package ai.improve;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ai.improve.log.IMPLog;

@RunWith(AndroidJUnit4.class)
public class DecisionTrackerTest {
    private static final String Tag = "DecisionTrackerTest";

    public static final String Track_URL = "https://gh8hd0ee47.execute-api.us-east-1.amazonaws.com/track";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
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
