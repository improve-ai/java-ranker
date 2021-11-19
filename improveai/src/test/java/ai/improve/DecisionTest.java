package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import ai.improve.log.IMPLog;

import static ai.improve.DecisionTrackerTest.Track_URL;

import org.junit.Before;
import org.junit.jupiter.api.Test;

public class DecisionTest {

    @Before
    public void setUp() throws Exception {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
    }

    @Test
    public void testGetWithoutModel() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        int loop = 10000;
        DecisionModel decisionModel = new DecisionModel("theme");
        for(int i = 0; i < loop; i++) {
            Decision decision = new Decision(decisionModel);
            String greeting = (String) decision.chooseFrom(variants).get();
            assertEquals(greeting, variants.get(0));
        }
    }

    @Test
    public void testChooseFrom() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        decision.chooseFrom(variants).get();

        List<Object> newVariants = new ArrayList<>();
        newVariants.add("HELLO WORLD!");
        Object variant = decision.chooseFrom(newVariants).get();
        assertEquals(variants.get(0), variant);
    }

    // Unit test that null or empty variants returns null on get()
    @Test
    public void testChooseFromNullVariants() {
        List<Object> variants = new ArrayList<>();
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(variants).get());
    }

    // Unit test that null or empty variants returns null on get()
    @Test
    public void testChooseFromEmptyVariants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(null).get());
    }

    @Test
    public void testTrack() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        decision.chooseFrom(variants).get();
    }
}
