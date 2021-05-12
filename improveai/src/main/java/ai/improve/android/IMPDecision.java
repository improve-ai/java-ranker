package ai.improve.android;

import java.util.List;
import java.util.Map;

public class IMPDecision {
    public static final String Tag = "IMPDecision";

    private IMPDecisionModel model;

    private List variants;

    private Map givens;

    private Map context;

    private boolean chosen;

    public IMPDecision(IMPDecisionModel model) {
        this.model = model;
    }

    public IMPDecision chooseFrom(List variants) {
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
}
