package ai.improve.android;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.improve.DecisionModel;
import ai.improve.DecisionTracker;
import ai.improve.GivensProvider;
import ai.improve.Decision;

import static ai.improve.android.DecisionTrackerTest.Tracker_Url;
import static org.junit.Assert.*;

public class DecisionTest {
    public class AppGivensProviderImp implements GivensProvider {
        @Override
        public Map<String, ?> getGivens() {
            return null;
        }
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
        DecisionModel decisionModel = new DecisionModel("");
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

        DecisionModel decisionModel = new DecisionModel("");
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
        DecisionModel decisionModel = new DecisionModel("");
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(variants).get());
    }

    // Unit test that null or empty variants returns null on get()
    @Test
    public void testChooseFromEmptyVariants() {
        DecisionModel decisionModel = new DecisionModel("");
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(null).get());
    }

    @Test
    public void testTrack() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        DecisionModel decisionModel = new DecisionModel("");
        decisionModel.track(new DecisionTracker(Tracker_Url));

        Decision decision = new Decision(decisionModel);
        decision.chooseFrom(variants).get();
    }
}
