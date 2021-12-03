package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static ai.improve.DecisionTracker.COUNT_KEY;
import static ai.improve.DecisionTracker.DECISION_BEST_KEY;
import static ai.improve.DecisionTracker.SAMPLE_VARIANT_KEY;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;
import ai.improve.util.ModelUtils;


import org.junit.jupiter.api.Test;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DecisionTrackerTest {
    public static final String Tag = "IMPDecisionModelTest";

    public static final String Track_URL = "https://gh8hd0ee47.execute-api.us-east-1.amazonaws.com/track";

    public static final String Track_Api_Key = "track-api-key";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    private DecisionTracker newTracker() {
        return new DecisionTracker(Track_URL, Track_Api_Key);
    }

    @Test
    public void testShouldTrackRunnersUp_0_variantsCount() {
        int variantCount = 0;
        int loop = 1000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);
        ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp());

        for(int i = 0; i < loop; ++i) {
            if(ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp())) {
                shouldTrackCount++;
            }
        }
        assertEquals(shouldTrackCount, 0);
    }

    @Test
    public void testShouldTrackRunnersUp_1_variantsCount() {
        int variantCount = 1;
        int loop = 1000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);
        ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp());

        for(int i = 0; i < loop; ++i) {
            if(ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp())) {
                shouldTrackCount++;
            }
        }
        assertEquals(shouldTrackCount, 0);
    }

    @Test
    public void testShouldTrackRunnersUp_10_variantsCount() {
        int variantCount = 10;
        int loop = 10000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);
        ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp());

        for(int i = 0; i < loop; ++i) {
            if(ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp())) {
                shouldTrackCount++;
            }
        }

        int expectedCount = (int)(1.0/Math.min(variantCount-1, tracker.getMaxRunnersUp()) * loop);
        System.out.println("expected=" + expectedCount + ", shouldTrackCount=" + shouldTrackCount);
        assertEquals(shouldTrackCount, expectedCount, 0.01 * expectedCount);
    }

    @Test
    public void testShouldTrackRunnersUp_100_variantsCount() {
        int variantCount = 100;
        int loop = 10000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);
        ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp());

        for(int i = 0; i < loop; ++i) {
            if(ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp())) {
                shouldTrackCount++;
            }
        }

        int expectedCount = (int)(1.0/Math.min(variantCount-1, tracker.getMaxRunnersUp()) * loop);
        System.out.println("expected=" + expectedCount + ", shouldTrackCount=" + shouldTrackCount);
        assertEquals(shouldTrackCount, expectedCount, 0.01 * expectedCount);
    }

    @Test
    public void testShouldTrackRunnersUp_0_maxRunnersUp() {
        int variantCount = 10;
        int loop = 1000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(0);
        ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp());

        for(int i = 0; i < loop; ++i) {
            if(ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp())) {
                shouldTrackCount++;
            }
        }
        assertEquals(shouldTrackCount, 0);
    }

    // If there are no runners up, then sample is a random sample from
    // variants with just best excluded.
    @Test
    public void testSetSampleVariant_0_RunnersUp() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(0);

        int runnersUpCount = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        Map<String, Integer> countMap = new HashMap<>();
        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            Map<String, Object> body = new HashMap();
            tracker.setSampleVariant(variants, runnersUpCount, true, "Hello, World!", body);
            String variant = (String)body.get(SAMPLE_VARIANT_KEY);

            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }

        int expectedCount = loop / (variants.size()-1-runnersUpCount);
        for(int i = 1+runnersUpCount; i < variants.size(); ++i){
            assertEquals(countMap.get(variants.get(i)), expectedCount, 0.01 * expectedCount);
        }
    }

    // If there are runners up, then sample is a random sample from
    // variants with best and runners up excluded.
    @Test
    public void testSampleVariant_2_RunnersUp() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(2);

        int runnersUpCount = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        Map<String, Integer> countMap = new HashMap<>();
        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            Map<String, Object> body = new HashMap();
            tracker.setSampleVariant(variants, runnersUpCount, true, "Hello, World!", body);
            String variant = (String)body.get(SAMPLE_VARIANT_KEY);

            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }

        int expectedCount = loop / (variants.size()-1-runnersUpCount);
        for(int i = 1+runnersUpCount; i < variants.size(); ++i){
            assertEquals(countMap.get(variants.get(i)), expectedCount, 0.01 * expectedCount);
        }
    }

    // If there is only one variant, which is the best, then there is no sample.
    @Test
    public void testSampleVariant_1_variant() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);

        int runnersUpCount = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        Map<String, Object> body = new HashMap();
        tracker.setSampleVariant(variants, runnersUpCount, true, "Hello, World!", body);
        assertFalse(body.containsKey(SAMPLE_VARIANT_KEY));
    }

    // If there are no remaining variants after best and runners up, then there is no sample.
    @Test
    public void testSampleVariant_0_remaining_variants() throws Exception {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);

        int runnersUpCount = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        Map<String, Object> body = new HashMap();
        tracker.setSampleVariant(variants, runnersUpCount, true, "Hello, World!", body);
        assertFalse(body.containsKey(SAMPLE_VARIANT_KEY));
    }

    /**
     * one best variant, one runners up and one null variant
     * */
    @Test
    public void testSampleVariant_with_null_variant() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("Hi");
        variants.add(null);

        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(1);

        int runnersUpCount = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        Map<String, Object> body = new HashMap();
        tracker.setSampleVariant(variants, runnersUpCount, true, "Hello, World!", body);
        assertTrue(body.containsKey(SAMPLE_VARIANT_KEY));
        assertNull(body.get(SAMPLE_VARIANT_KEY));
    }

    @Test
    public void testSampleVariant_not_ranked() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        DecisionTracker tracker = newTracker();

        int runnersUpCount = 0;
        String bestVariant = "hello, world!";

        Map<String, Integer> countMap = new HashMap<>();
        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            Map<String, Object> body = new HashMap();
            tracker.setSampleVariant(variants, runnersUpCount, false, bestVariant, body);
            String variant = (String)body.get(SAMPLE_VARIANT_KEY);

            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }

        assertFalse(countMap.containsKey(bestVariant));

        int expectedCount = loop / (variants.size()-1);
        assertEquals(countMap.get("Hello, World!"), expectedCount, 0.01 * expectedCount);
        assertEquals(countMap.get("hello"), expectedCount, 0.01 * expectedCount);
        assertEquals(countMap.get("hi"), expectedCount, 0.01 * expectedCount);
        assertEquals(countMap.get("Hello World!"), expectedCount, 0.01 * expectedCount);
    }

    @Test
    public void testSampleVariant_not_ranked_identical_variants() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("Hello, World!");
        variants.add("Hello, World!");
        variants.add("Hello, World!");
        variants.add("Hello, World!");

        DecisionTracker tracker = newTracker();

        int runnersUpCount = 0;
        String bestVariant = "Hello, World!";

        Map<String, Integer> countMap = new HashMap<>();
        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            Map<String, Object> body = new HashMap();
            tracker.setSampleVariant(variants, runnersUpCount, false, bestVariant, body);
            String variant = (String)body.get(SAMPLE_VARIANT_KEY);

            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }

        assertEquals(countMap.get("Hello, World!"), loop);
    }

    /**
     * Make sure that null variant is correctly json encoded
     * */
    @Test
    public void testSetBestVariantNil() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);

        Map<String, Object> body = new HashMap();
        tracker.setBestVariant(null, body);
        assertTrue(body.containsKey(DECISION_BEST_KEY));
        assertEquals(new GsonBuilder().serializeNulls().create().toJson(body), "{\"variant\":null}");

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> variant = new HashMap<>();
        variant.put("theme", null);
        variant.put("font", null);
        variant.put("color", "#f0f0f0");
        root.put("variant", variant);
        IMPLog.d(Tag, new GsonBuilder().serializeNulls().create().toJson(root));
    }

    @Test
    public void testTopRunnersUp_Null_runner_up() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(2);

        List variants = Arrays.asList("foo", null, "bar");
        List topRunnersUp = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertNotNull(topRunnersUp);
        assertEquals(2, topRunnersUp.size());
    }

    @Test
    public void testTopRunnersUp_1_variant() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);

        int numOfVariants = 1;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        List<Object> topRunnersUp = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertEquals(topRunnersUp.size(), 0);
    }

    @Test
    public void testTopRunnersUp_2_variants() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);

        int numOfVariants = 2;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        List<Object> topRunnersUp = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertEquals(topRunnersUp.size(), 1);
    }

    @Test
    public void testTopRunnersUp_10_variants() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);

        int numOfVariants = 10;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        List<Object> topRunnersUp = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertEquals(topRunnersUp.size(), 9);

        for(int i = 0; i < topRunnersUp.size(); i++) {
            assertEquals(topRunnersUp.get(i), i+1);
        }
    }

    @Test
    public void testTopRunnersUp_100_variants() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(50);

        int numOfVariants = 100;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        List<Object> topRunnersUp = tracker.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertEquals(topRunnersUp.size(), 50);

        for(int i = 0; i < topRunnersUp.size(); i++) {
            assertEquals(topRunnersUp.get(i), i+1);
        }
    }

    @Test
    public void testSetBestVariantNonNil() {
        DecisionTracker tracker = newTracker();
        Map<String, Object> body = new HashMap();
        tracker.setBestVariant("hello", body);
        assertEquals("hello", body.get(DECISION_BEST_KEY));
    }

    @Test
    public void testSetCount_2_variants() {
        DecisionTracker tracker = newTracker();
        int numOfVariants = 2;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        Map<String, Object> body = new HashMap();
        tracker.setCount(variants, body);

        assertEquals(2, body.get(COUNT_KEY));
    }

    @Test
    public void testSetCount_null_variants() {
        DecisionTracker tracker = newTracker();

        List<Object> variants = null;

        Map<String, Object> body = new HashMap();
        tracker.setCount(variants, body);

        assertEquals(1, body.get(COUNT_KEY));
    }

    @Test
    public void testSetCount_empty_variants() {
        DecisionTracker tracker = newTracker();

        List<Object> variants = new ArrayList<>();

        Map<String, Object> body = new HashMap();
        tracker.setCount(variants, body);

        assertEquals(1, body.get(COUNT_KEY));
    }

    @Test
    public void testSetMaxRunnersUp() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(10);
        assertEquals(10, tracker.getMaxRunnersUp());
    }

    @Test
    public void testSetMaxRunnersUp_NegativeValue() {
        DecisionTracker tracker = newTracker();
        tracker.setMaxRunnersUp(-1);
        assertEquals(0, tracker.getMaxRunnersUp());
    }
}