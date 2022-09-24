package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import ai.improve.log.IMPLog;

import static ai.improve.DecisionModelTest.DefaultFailMessage;
import static ai.improve.DecisionTrackerTest.Track_URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DecisionTest {
    @BeforeAll
    public static void setUp() {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
    }

    private List<String> variants() {
        return Arrays.asList("Hello", "Hi", "Hey");
    }

    private DecisionModel model() {
        return new DecisionModel("greetings");
    }

    @Test
    public void testGet() {
        List<String> variants = Arrays.asList("hi", "hello", "Hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        // Unit test that no type cast needed here
        String greeting = decisionModel.decide(variants).get();
        assertEquals("hi", greeting);
    }

    @Test
    public void testGet_without_loading_model() {
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        for(int i = 0; i < 100; ++i) {
            String greeting = decisionModel.decide(variants).get();
            assertEquals(greeting, variants.get(0));
        }
    }

    @Test
    public void testRanked() {
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        List<String> rankedVariants = model().decide(variants).ranked();
        assertEquals(variants, rankedVariants);
    }

    @Test
    public void testTrack() {
        Decision decision = model().decide(variants());
        assertNull(decision.id);
        decision.track();
        assertNotNull(decision.id);
        assertTrue(!decision.id.isEmpty());
    }

    @Test
    public void testTrack_nil_trackURL() {
        DecisionModel decisionModel = model();
        decisionModel.setTrackURL(null);
        Decision decision = decisionModel.decide(variants());
        try {
            decision.track();
            fail("trackURL not set for the underlying DecisionModel.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            assertEquals("trackURL not set for the underlying DecisionModel!", e.getMessage());
        }
    }

    @Test
    public void testTrack_called_twice() {
        Decision decision = model().decide(variants());
        decision.track();
        try {
            decision.track();
            fail("decision already tracked.");
        } catch (IllegalStateException e){
            assertEquals("the decision is already tracked!", e.getMessage());
        }
    }

    @Test
    public void testAddReward_before_get() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = decisionModel.decide(variants());
            decision.addReward(0.1);
            fail("addReward() can't be called before track().");
        } catch (IllegalStateException e) {
            assertEquals("addReward() can't be called before track().", e.getMessage());
            return ;
        }
    }

    @Test
    public void testAddReward_nil_trackURL() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.decide(Arrays.asList(1, 2, 3));
        decision.track();
        decisionModel.setTrackURL(null);
        try {
            decision.addReward(0.1);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            assertEquals("trackURL can't be null when calling addReward()", e.getMessage());
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_NaN() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = decisionModel.chooseFrom(Arrays.asList(1, 2, 3));
            decision.track();
            decision.addReward(Double.NaN);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_positive_infinity() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseFrom(Arrays.asList(1, 2, 3));
        decision.track();
        try {
            decision.addReward(Double.POSITIVE_INFINITY);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_negative_infinity() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseFrom(Arrays.asList(1, 2, 3));
        decision.track();
        try {
            decision.addReward(Double.NEGATIVE_INFINITY);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }
}
