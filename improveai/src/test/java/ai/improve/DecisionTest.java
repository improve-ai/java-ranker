package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.improve.log.IMPLog;

import static ai.improve.DecisionModelTest.DefaultFailMessage;
import static ai.improve.DecisionModelTest.Tag;
import static ai.improve.DecisionTrackerTest.Track_URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DecisionTest {

    @BeforeAll
    public static void setUp() {
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

        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        String greeting = (String) decision.chooseFrom(variants).get();
        assertEquals(greeting, variants.get(0));
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

    @Test
    public void testChooseFromEmptyVariants() {
        try {
            List<Object> variants = new ArrayList<>();
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            assertNull(decision.chooseFrom(variants).get());
        } catch (IllegalStateException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromNullVariants() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            assertNull(decision.chooseFrom(null).get());
        } catch (IllegalStateException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromVariantsWithNull() {
        List variants = new ArrayList();
        variants.add(null);
        variants.add("Hello");
        variants.add("Hi");
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(variants).get());
    }

    @Test
    public void testTrack() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        decision.chooseFrom(variants).get();
    }

    @Test
    public void testAddReward_before_get() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            decision.addReward(0.1);
        } catch (IllegalStateException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_nil_trackURL() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);

            // set trackURL to null
            decisionModel.setTrackURL(null);

            decision.chooseFrom(Arrays.asList(1, 2, 3)).get();
            decision.addReward(0.1);
        } catch (IllegalStateException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_NaN() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            decision.chooseFrom(Arrays.asList(1, 2, 3)).get();
            decision.addReward(Double.NaN);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_positive_infinity() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            decision.chooseFrom(Arrays.asList(1, 2, 3)).get();
            decision.addReward(Double.POSITIVE_INFINITY);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_negative_infinity() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            decision.chooseFrom(Arrays.asList(1, 2, 3)).get();
            decision.addReward(Double.NEGATIVE_INFINITY);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_no_argument() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.which();
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }


    @Test
    public void testWhich_1_argument_non_array() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.which(1);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_1_argument_array() {
        DecisionModel decisionModel = new DecisionModel("theme");
        decisionModel.which(Arrays.asList(1));
    }

    @Test
    public void testWhich_multiple_arguments() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Object best = decisionModel.which(Arrays.asList(1, 2, 3), 2, 3, "hello");
        IMPLog.d(Tag, "best is " + best);
    }
}
