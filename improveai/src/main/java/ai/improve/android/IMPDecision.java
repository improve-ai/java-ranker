package ai.improve.android;

import java.util.List;
import java.util.Map;

public class IMPDecision {
    public static final String Tag = "IMPDecision";

    private IMPDecisionModel model;

    private List<Object> variants;

    private Map<String, Object> givens;

    private boolean chosen;

    private Object best;

    public IMPDecision(IMPDecisionModel model) {
        this.model = model;
    }

    public IMPDecision chooseFrom(List<Object> variants) {
        if(chosen) {
            IMPLog.e(Tag, "variant already chosen, ignoring variants");
        } else {
            this.variants = variants;
        }
        return this;
    }

    public IMPDecision given(Map givens) {
        if(chosen) {
            IMPLog.e(Tag, "variant already chosen, ignoring givens");
        } else {
            this.givens = givens;
        }
        return this;
    }

    public Object get() {
        if(chosen) {
            return best;
        }

        List<Double> scores = model.score(variants, givens);

        if(variants != null && variants.size() > 0) {
            IMPDecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                if(tracker.shouldtrackRunnersUp(variants.size())) {
                    // the more variants there are, the less frequently this is called
                    List<Object> rankedVariants = IMPDecisionModel.rank(variants, scores);
                    best = rankedVariants.get(0);
                    model.getTracker().track(best, variants, givens, model.getModelName(), true);
                } else {
                    // faster and more common path, avoids array sort
                    best = IMPDecisionModel.topScoringVariant(variants, scores);
                    model.getTracker().track(best, variants, givens, model.getModelName(), false);
                }
            }
        } else {
            // Unit test that "variant": null JSON is tracked on null or empty variants.
            // "count" field should be 1
            best = null;
            model.getTracker().track(best, variants, givens, model.getModelName(), false);
        }

        chosen = true;

        return best;
    }
}
