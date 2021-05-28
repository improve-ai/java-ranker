package ai.improve;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IMPDecision {
    public static final String Tag = "IMPDecision";

    private BaseIMPDecisionModel model;

    private List<?> variants;

    private Map<String, Object> givens;

    private boolean chosen;

    private Object best;

    public IMPDecision(BaseIMPDecisionModel model) {
        this.model = model;
    }

//    public <T> IMPDecision chooseFrom(T[] variants) {
//        return chooseFrom(Arrays.asList(variants));
//    }

    public <T> IMPDecision chooseFrom(List<T> variants) {
        if(chosen) {
            IMPLog.e(Tag, "variant already chosen, ignoring variants");
        } else {
            this.variants = variants;
        }
        return this;
    }

    public IMPDecision given(Map<String, Object> givens) {
        if(chosen) {
            IMPLog.e(Tag, "variant already chosen, ignoring givens");
        } else {
            this.givens = givens;
        }
        return this;
    }

    public synchronized Object get() {
        if(chosen) {
            return best;
        }

        List<Double> scores = model.score(variants, givens);

        if(variants != null && variants.size() > 0) {
            BaseIMPDecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                if(IMPUtils.shouldtrackRunnersUp(variants.size(), tracker.getMaxRunnersUp())) {
                    // the more variants there are, the less frequently this is called
                    List<Object> rankedVariants = BaseIMPDecisionModel.rank(variants, scores);
                    best = rankedVariants.get(0);
                    IMPTrackerHandler.track(tracker, best, variants, givens, model.getModelName(), true);
                } else {
                    // faster and more common path, avoids array sort
                    best = IMPUtils.topScoringVariant(variants, scores);
                    IMPTrackerHandler.track(tracker, best, variants, givens, model.getModelName(), false);
                }
            } else {
                best = IMPUtils.topScoringVariant(variants, scores);
            }
        } else {
            // Unit test that "variant": null JSON is tracked on null or empty variants.
            // "count" field should be 1
            best = null;
            BaseIMPDecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                IMPTrackerHandler.track(tracker, best, variants, givens, model.getModelName(), false);
            }
        }

        chosen = true;

        return best;
    }
}
