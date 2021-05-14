package ai.improve.android;

import java.util.List;
import java.util.Map;

public class IMPDecision {
    public static final String Tag = "IMPDecision";

    private IMPDecisionModel model;

    private List<Object> variants;

    private Map<String, ?> givens;

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

        } else {
            // Unit test that "variant": null JSON is tracked on null or empty variants.
            // "count" field should be 1
            best = null;
        }

        chosen = true;

        return best;
    }
}
