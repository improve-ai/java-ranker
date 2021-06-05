package ai.improve.android;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.BaseDecisionTracker;
import ai.improve.HistoryIdProvider;
import ai.improve.TrackerHandler;
import ai.improve.ModelUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class BaseDecisionTrackerTest {
    public static final String Tag = "IMPDecisionModelTest";

    private class DecisionTracker extends BaseDecisionTracker {

        public DecisionTracker(String trackURL, HistoryIdProvider historyIdProvider) {
            this(trackURL, "", historyIdProvider);
        }

        public DecisionTracker(String trackURL, String apiKey, HistoryIdProvider historyIdProvider) {
            super(trackURL, apiKey, historyIdProvider);
        }
    }

    public class HistoryIdProviderImp implements HistoryIdProvider {
        @Override
        public String getHistoryId() {
            return "android_test_history_id";
        }
    }

    @Test
    public void testShouldTrackRunnersUp_0_variantsCount() {
        int variantCount = 0;
        int loop = 1000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
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

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
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

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);
        ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp());

        for(int i = 0; i < loop; ++i) {
            if(ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp())) {
                shouldTrackCount++;
            }
        }

        int expectedCount = (int)(1.0/Math.min(variantCount-1, tracker.getMaxRunnersUp()) * loop);
        double diff = Math.abs((expectedCount-shouldTrackCount)/(double)expectedCount);
        assertTrue(diff < 0.01);
        System.out.println("expected=" + expectedCount + ", real=" + shouldTrackCount
                + ", diff=" + (expectedCount-shouldTrackCount)/(double)expectedCount);
    }

    @Test
    public void testShouldTrackRunnersUp_100_variantsCount() {
        int variantCount = 100;
        int loop = 10000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);
        ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp());

        for(int i = 0; i < loop; ++i) {
            if(ModelUtils.shouldtrackRunnersUp(variantCount, tracker.getMaxRunnersUp())) {
                shouldTrackCount++;
            }
        }

        int expectedCount = (int)(1.0/Math.min(variantCount-1, tracker.getMaxRunnersUp()) * loop);
        double diff = Math.abs((expectedCount-shouldTrackCount)/(double)expectedCount);
        System.out.println("expected=" + expectedCount + ", real=" + shouldTrackCount
                + ", diff=" + (expectedCount-shouldTrackCount)/(double)expectedCount);
        assertTrue(diff < 0.01);
    }

    @Test
    public void testShouldTrackRunnersUp_0_maxRunnersUp() {
        int variantCount = 10;
        int loop = 1000000;
        int shouldTrackCount = 0;

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
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
    public void testSampleVariant_0_RunnersUp() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(0);

        int runnersUpCount = TrackerHandler.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        Map<String, Integer> countMap = new HashMap<>();
        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            String variant = (String) TrackerHandler.sampleVariant(variants, runnersUpCount);
            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }

        int expectedCount = loop / (variants.size()-1-runnersUpCount);
        for(int i = 1+runnersUpCount; i < variants.size(); ++i){
            assertTrue(countMap.containsKey(variants.get(i)));
            int diff = Math.abs(countMap.get(variants.get(i)) - expectedCount);
            System.out.println("expected=" + expectedCount + ", real=" + countMap.get(variants.get(i))
                    + ", diff=" + diff);
            assertTrue(diff < (expectedCount * 0.01));
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

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(2);

        int runnersUpCount = TrackerHandler.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        Map<String, Integer> countMap = new HashMap<>();
        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            String variant = (String) TrackerHandler.sampleVariant(variants, runnersUpCount);
            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }

        int expectedCount = loop / (variants.size()-1-runnersUpCount);
        for(int i = 1+runnersUpCount; i < variants.size(); ++i){
            assertTrue(countMap.containsKey(variants.get(i)));
            int diff = Math.abs(countMap.get(variants.get(i)) - expectedCount);
            System.out.println("expected=" + expectedCount + ", real=" + countMap.get(variants.get(i))
                    + ", diff=" + diff);
            assertTrue(diff < (expectedCount * 0.01));
        }
    }

    // If there is only one variant, which is the best, then there is no sample.
    @Test
    public void testSampleVariant_1_variant() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);

        int runnersUpCount = TrackerHandler.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            String variant = (String) TrackerHandler.sampleVariant(variants, runnersUpCount);
            assertNull(variant);
        }
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

        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);

        int runnersUpCount = TrackerHandler.topRunnersUp(variants, tracker.getMaxRunnersUp()).size();
        System.out.println("runnersUpCount=" + runnersUpCount);

        int loop = 10000000;
        for(int i = 0; i < loop; ++i) {
            String variant = (String) TrackerHandler.sampleVariant(variants, runnersUpCount);
            assertNull(variant);
        }
    }

    @Test
    public void testSetBestVariantNil() {
        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);

        Map<String, Object> body = new HashMap();
        TrackerHandler.setBestVariant(null, body);
        // body looks like this
        // {
        //     "count" : 1,
        //     "variant" : null
        // }
        assertNull(body.get("variant"));
        assertEquals(1, body.get("count"));
    }

    @Test
    public void testTopRunnersUp_1_variant() {
        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);

        int numOfVariants = 1;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        List<Object> topRunnersUp = TrackerHandler.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertEquals(topRunnersUp.size(), 0);
    }

    @Test
    public void testTopRunnersUp_10_variants() {
        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);

        int numOfVariants = 10;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        List<Object> topRunnersUp = TrackerHandler.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertEquals(topRunnersUp.size(), 9);

        for(int i = 0; i < topRunnersUp.size(); i++) {
            assertEquals(topRunnersUp.get(i), i+1);
        }
    }

    @Test
    public void testTopRunnersUp_100_variants() throws Exception {
        DecisionTracker tracker = new DecisionTracker("", new HistoryIdProviderImp());
        tracker.setMaxRunnersUp(50);

        int numOfVariants = 100;
        List<Object> variants = new ArrayList<>();
        for(int i = 0; i < numOfVariants; i++) {
            variants.add(i);
        }

        List<Object> topRunnersUp = TrackerHandler.topRunnersUp(variants, tracker.getMaxRunnersUp());
        assertEquals(topRunnersUp.size(), 50);

        for(int i = 0; i < topRunnersUp.size(); i++) {
            assertEquals(topRunnersUp.get(i), i+1);
        }
    }

    @Test
    public void testSetBestVariantNonNil() throws Exception {
        Map<String, Object> body = new HashMap();
        TrackerHandler.setBestVariant("hello", body);

        assertEquals("hello", body.get("variant"));
    }
}