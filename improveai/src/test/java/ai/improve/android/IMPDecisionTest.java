package ai.improve.android;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import ai.improve.BaseIMPDecisionModel;
import ai.improve.BaseIMPDecisionTracker;
import ai.improve.HistoryIdProvider;
import ai.improve.IMPDecision;
import ai.improve.XXHashProvider;

import static org.junit.Assert.*;

public class IMPDecisionTest {
    public class IMPDecisionModel extends BaseIMPDecisionModel {
        public IMPDecisionModel(String modelName, XXHashProvider xxHashProvider) {
            super(modelName, xxHashProvider);
        }
    }

    public class XXHashProviderImp implements XXHashProvider {

        @Override
        public long xxhash(byte[] data, long seed) {
            return 0;
        }
    }

    private class IMPDecisionTracker extends BaseIMPDecisionTracker {
        public IMPDecisionTracker(String trackURL, HistoryIdProvider historyIdProvider) {
            super(trackURL, historyIdProvider);
        }

        public IMPDecisionTracker(String trackURL, String apiKey, HistoryIdProvider historyIdProvider) {
            super(trackURL, apiKey, historyIdProvider);
        }
    }

    @Test
    public void testGetWithoutVariants() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        int loop = 10000;
        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        for(int i = 0; i < loop; i++) {
            IMPDecision decision = new IMPDecision(decisionModel);
            String greeting = (String) decision.chooseFrom(variants).get();
            assertEquals(greeting, variants.get(0));
        }
    }

    @Test
    public void testChooseFrom() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        IMPDecision decision = new IMPDecision(decisionModel);
        decision.chooseFrom(variants).get();

        List<Object> newVariants = new ArrayList<>();
        newVariants.add("HELLO WORLD!");
        Object variant = decision.chooseFrom(newVariants).get();
        assertEquals(variants.get(0), variant);
    }

    @Test
    public void testChooseFromNullVariants() {
        List<Object> variants = new ArrayList<>();
        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        IMPDecision decision = new IMPDecision(decisionModel);
        assertNull(decision.chooseFrom(variants).get());
    }

    @Test
    public void testChooseFromEmptyVariants() {
        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        IMPDecision decision = new IMPDecision(decisionModel);
        assertNull(decision.chooseFrom(null).get());
    }

    @Test
    public void testTrack() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        decisionModel.track(new IMPDecisionTracker("http://trakcer.url", new HistoryIdProvider() {
            @Override
            public String getHistoryId() {
                return "test-history-id";
            }
        }));

        IMPDecision decision = new IMPDecision(decisionModel);
        decision.chooseFrom(variants).get();
    }
}
