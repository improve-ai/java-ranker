package ai.improve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Date;
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
    public void testTrackerNullVariants() {
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.which(Arrays.asList(1, null));
    }

    /**
     * No assertion here.
     * Just a convenient place to observer log output of non-json-encodable objects tracking
     * */
    @Test
    public void testTrackNonJsonEncodable() {
        Map<String, Object> givens = new HashMap<>();
        givens.put("font", 12);
        givens.put("color", "#ffffff");
        givens.put("date", new Date());
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.given(givens).which(Arrays.asList("Hello World", "Howdy World", "Yo World"));
    }

    @Test
    public void testTrack_persist_decision_id() {
        String modelName = "hello";
        DecisionModel decisionModel = new DecisionModel(modelName);
        Decision<Integer> decision = decisionModel.decide(Arrays.asList(1, 2, 3));

        String decisionIdBeforeGet = decisionModel.getTracker().lastDecisionIdOfModel(modelName);
        decision.track();
        String decisionIdAfterGet = decisionModel.getTracker().lastDecisionIdOfModel(modelName);

        assertNotNull(decision.id);
        assertEquals(decision.id, decisionIdAfterGet);
        assertNotEquals(decisionIdBeforeGet, decisionIdAfterGet);
        IMPLog.d(Tag, "decisionId: " + decision.id + ", " + decisionIdBeforeGet +
                ", " + decisionIdAfterGet);
    }
}
