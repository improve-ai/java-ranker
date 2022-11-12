package ai.improve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static ai.improve.DecisionModelTest.ModelURL;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

@RunWith(AndroidJUnit4.class)
public class DecisionTest {
    public static final String Tag = "DecisionContextTest";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    public DecisionModel model() {
        return new DecisionModel("greetings");
    }

    private Map<String, String> givens() {
        Map<String, String> givens = new HashMap<>();
        givens.put("lang", "en");
        return givens;
    }

    private List<String> variants() {
        return Arrays.asList("Hello", "Hi", "Hey");
    }

    @Test
    public void testId() {
        Decision<String> decision = model().decide(variants());
        assertNull(decision.getId());
        decision.track();
        assertNotNull(decision.getId());
    }

    @Test
    public void testGivens() {
        Decision<String> decision = model().decide(variants());
        assertEquals(20, decision.givens.size());
    }

    @Test
    public void testGet_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        Decision decision = decisionModel.decide(variants());
        String d0 = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decision.get();
        String d1 = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decision.get();
        String d2 = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decision id: " + d0 + ", " + d1 + ", " + d2);
        assertNotNull(d0);
        assertNotNull(d1);
        assertNotEquals(d0, d1);
        assertEquals(d1, d2);
    }
}
