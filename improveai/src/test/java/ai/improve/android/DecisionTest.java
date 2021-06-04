package ai.improve.android;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import ai.improve.BaseDecisionModel;
import ai.improve.BaseDecisionTracker;
import ai.improve.HistoryIdProvider;
import ai.improve.Decision;
import ai.improve.XXHashProvider;

import static org.junit.Assert.*;

public class DecisionTest {
    public class DecisionModel extends BaseDecisionModel {
        public DecisionModel(String modelName, XXHashProvider xxHashProvider) {
            super(modelName, xxHashProvider);
        }
    }

    public class XXHashProviderImp implements XXHashProvider {

        @Override
        public long xxhash(byte[] data, long seed) {
            return 0;
        }
    }

    private class DecisionTracker extends BaseDecisionTracker {
        public DecisionTracker(String trackURL, HistoryIdProvider historyIdProvider) {
            super(trackURL, historyIdProvider);
        }

        public DecisionTracker(String trackURL, String apiKey, HistoryIdProvider historyIdProvider) {
            super(trackURL, apiKey, historyIdProvider);
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
        DecisionModel decisionModel = new DecisionModel("", new XXHashProviderImp());
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

        DecisionModel decisionModel = new DecisionModel("", new XXHashProviderImp());
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
        DecisionModel decisionModel = new DecisionModel("", new XXHashProviderImp());
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(variants).get());
    }

    // Unit test that null or empty variants returns null on get()
    @Test
    public void testChooseFromEmptyVariants() {
        DecisionModel decisionModel = new DecisionModel("", new XXHashProviderImp());
        Decision decision = new Decision(decisionModel);
        assertNull(decision.chooseFrom(null).get());
    }

    @Test
    public void testTrack() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        DecisionModel decisionModel = new DecisionModel("", new XXHashProviderImp());
        decisionModel.track(new DecisionTracker("http://trakcer.url", new HistoryIdProvider() {
            @Override
            public String getHistoryId() {
                return "test-history-id";
            }
        }));

        Decision decision = new Decision(decisionModel);
        decision.chooseFrom(variants).get();
    }
}
