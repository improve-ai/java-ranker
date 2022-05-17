package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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

    private List variants() {
        return Arrays.asList("Hello", "Hi", "Hey");
    }

    @Test
    public void testPeek() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Object best = decisionModel.chooseFrom(variants()).peek();
        assertNotNull(best);
    }

    @Test
    public void testGet_track_only_once() throws InterruptedException {
        DecisionModel decisionModel = new DecisionModel("theme");
        assertNotNull(decisionModel.getTracker());

        Decision decision = decisionModel.chooseFrom(variants());
        assertEquals(0, decision.tracked);

        Semaphore semaphore = new Semaphore(0);
        AtomicInteger count = new AtomicInteger(0);
        int loop = 100;
        for(int i = 0; i < loop; i++) {
            new Thread() {
                @Override
                public void run() {
                    decision.get();
                    int cur = count.incrementAndGet();
                    if(cur == loop) {
                        semaphore.release();
                    }
                }
            }.start();
        }
        semaphore.acquire();
        assertEquals(1, decision.tracked);
    }

    @Test
    public void testGet_without_tracker() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseFrom(variants());
        assertEquals(0, decision.tracked);
        decision.get();
        assertEquals(1, decision.tracked);

        decision = decisionModel.chooseFrom(variants());
        decisionModel.setTrackURL(null);
        assertEquals(0, decision.tracked);
        decision.get();
        assertEquals(0, decision.tracked);
    }

    @Test
    public void testGet_without_loading_model() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseFrom(variants);
        String greeting = (String) decision.get();
        assertEquals(greeting, variants.get(0));
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
        DecisionModel decisionModel = new DecisionModel("theme");
        decisionModel.setTrackURL(null);
        Decision decision = decisionModel.chooseFrom(Arrays.asList(1, 2, 3));
        // set trackURL to null
        decision.get();
        try {
            decision.addReward(0.1);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_NaN() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = decisionModel.chooseFrom(Arrays.asList(1, 2, 3));
            decision.get();
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
        decision.get();
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
        decision.get();
        try {
            decision.addReward(Double.NEGATIVE_INFINITY);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testGet_generic() {
        List<String> variants = Arrays.asList("hi", "hello", "Hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        // Unit test that no type cast needed here
        String greeting = decisionModel.chooseFrom(variants).get();
        assertEquals("hi", greeting);
    }

    @Test
    public void testPeek_generic() {
        List<String> variants = Arrays.asList("hi", "hello", "Hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        // Unit test that no type cast needed here
        String greeting = decisionModel.chooseFrom(variants).peek();
        assertEquals("hi", greeting);
    }
}
