package ai.improve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

@RunWith(AndroidJUnit4.class)
public class DecisionContextTest {
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
    public void testGivens() {
        DecisionModel decisionModel = model();
        Decision decision = decisionModel.given(givens()).decide(variants());
        assertEquals(21, decision.givens.size());

        // If givensProvider is null, givens set by calling given() will still be used.
        decisionModel.setGivensProvider(null);
        decision = decisionModel.given(givens()).decide(variants());
        assertEquals(1, decision.givens.size());
    }

    @Test
    public void testChooseFrom() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        List variants = Arrays.asList("Hello", "Hi", "Hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFrom(variants);
        assertNotNull(decision);
        assertEquals("en", decision.givens.get("lang"));
        assertEquals(21, decision.givens.size());
    }

    @Test
    public void testChooseFirst() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFirst(variants);
        assertEquals("en", decision.givens.get("lang"));
        assertEquals(21, decision.givens.size());
        assertEquals("hi", decision.get());
    }

    @Test
    public void testFirst() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.given(givens).first("hi", "hello", "hey");
        assertEquals("hi", first);
    }

    @Test
    public void testChooseRandom() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFirst(variants);
        assertEquals("en", decision.givens.get("lang"));
        assertEquals(21, decision.givens.size());
    }

    @Test
    public void testRandom() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.given(givens).random(variants);
    }

    @Test
    public void testChooseFromVaiantsAndScores() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        List scores = Arrays.asList(0.05, 0.1, 0.08);
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFrom(variants, scores);
        assertEquals("hello", decision.get());
        assertEquals(21, decision.givens.size());
        assertEquals("en", decision.givens.get("lang"));
    }

    // Tests that decision is tracked when calling which().
    @Test
    public void testWhich_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).which(1, 2, 3);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown.
    @Test
    public void testWhich_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).which(1, 2, 3);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is tracked when calling whichFrom().
    @Test
    public void testWhichFrom_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).whichFrom(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown.
    @Test
    public void testWhichFrom_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).whichFrom(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked when calling rank().
    @Test
    public void testRank_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).rank(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown
    @Test
    public void testRank_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).rank(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is tracked when calling optimize().
    @Test
    public void testOptimize_track() {
        Map<String, List> variantMap = new HashMap<>();
        variantMap.put("font", Arrays.asList(12, 13));
        variantMap.put("color", Arrays.asList("white", "black"));
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).optimize(variantMap);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown
    @Test
    public void testOptimize_null_trackURL() {
        Map<String, List> variantMap = new HashMap<>();
        variantMap.put("font", Arrays.asList(12, 13));
        variantMap.put("color", Arrays.asList("white", "black"));
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.given(givens()).optimize(variantMap);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    @Test
    public void testTrack() throws InterruptedException {
        String variant = "hi";
        List<String> runnersUp = Arrays.asList("hello", "hey");
        String sample = "ha";
        int samplePoolSize = 4;
        String decisionId = model().given(givens()).track(variant, runnersUp, sample, samplePoolSize);
        assertNotNull(decisionId);
        Thread.sleep(3000);
    }
}
