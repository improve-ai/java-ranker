package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public void testSetGivens() {
        Map givens = new HashMap<String, Object>(){{
            put("language", "cowboy");
        }};

        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.given(givens);
        assertEquals(givens, decision.getGivens());
    }

    @Test
    public void testGivens_set_after_chooseFrom() {
        Map givens = new HashMap<String, Object>(){{
            put("language", "cowboy");
        }};
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseFrom(variants());
        assertNull(decision.getGivens());
        decision.setGivens(givens);
        assertNull(decision.getGivens());
    }

    @Test
    public void testChooseFrom() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        assertNull(decision.best);
        assertEquals(0, decision.chosen);
        assertNull(decision.allGivens);
        assertNull(decision.scores);
        decision.chooseFrom(variants());
        assertNotNull(decision.best);
        assertEquals(1, decision.chosen);
        assertNotNull(decision.allGivens);
        assertNotNull(decision.scores);
    }

    @Test
    public void testChooseFrom_empty_variants() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            decision.chooseFrom(new ArrayList<>());
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFrom_null_variants() {
        try {
            DecisionModel decisionModel = new DecisionModel("theme");
            Decision decision = new Decision(decisionModel);
            decision.chooseFrom(null);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFrom_null_variant() {
        List variants = new ArrayList();
        variants.add(null);
        variants.add("Hello");
        variants.add("Hi");
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(variants).get());
    }

    @Test
    public void testChooseFrom_choose_only_once() throws InterruptedException {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);

        int loop = 100;
        for(int i = 0; i < loop; ++i) {
            new Thread(){
                @Override
                public void run() {
                    decision.chooseFrom(variants());
                }
            }.start();
        }
        Thread.sleep(1000);
        assertEquals(1, decision.chosen);
    }

    @Test
    public void testPeek_before_chooseFrom() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        try {
            decision.peek();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testPeek() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Object best = decisionModel.chooseFrom(variants()).peek();
        assertNotNull(best);
    }

    @Test
    public void testGet_before_chooseFrom() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = new Decision(decisionModel);
        try {
            decision.get();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
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
}
