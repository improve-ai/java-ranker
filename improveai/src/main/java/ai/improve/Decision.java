package ai.improve;

import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;
import ai.improve.util.ModelUtils;

public class Decision {
    public static final String Tag = "Decision";

    private DecisionModel model;

    private List<?> variants;

    private Map<String, Object> givens;

    private boolean chosen;

    private Object best;

    public Decision(DecisionModel model) {
        this.model = model;
    }

    public <T> Decision chooseFrom(List<T> variants) {
        if(chosen) {
            IMPLog.e(Tag, "variant already chosen, ignoring variants");
        } else {
            this.variants = variants;
        }
        return this;
    }

    public Decision given(Map<String, Object> givens) {
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
            DecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                if(ModelUtils.shouldtrackRunnersUp(variants.size(), tracker.getMaxRunnersUp())) {
                    // the more variants there are, the less frequently this is called
                    List<Object> rankedVariants = DecisionModel.rank(variants, scores);
                    best = rankedVariants.get(0);
                    tracker.track(best, variants, givens, model.getModelName(), true);
                } else {
                    // faster and more common path, avoids array sort
                    best = ModelUtils.topScoringVariant(variants, scores);
                    tracker.track(best, variants, givens, model.getModelName(), false);
                }
            } else {
                best = ModelUtils.topScoringVariant(variants, scores);
                IMPLog.e(Tag, "tracker not set on DecisionModel, decision will not be tracked");
            }
        } else {
            // Unit test that "variant": null JSON is tracked on null or empty variants.
            // "count" field should be 1
            best = null;
            DecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                tracker.track(best, variants, givens, model.getModelName(), false);
            } else {
                IMPLog.e(Tag, "tracker not set on DecisionModel, decision will not be tracked");
            }
        }

        chosen = true;

        return best;
    }
}