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

    // The message_id of the tracked decision
    private String id;

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

    /**
     * Returns the chosen variant. The chosen variant will be memoized, so same value is returned
     * on subsequent calls.
     * @throws IllegalStateException Thrown if variants is null or empty.
     * */
    public synchronized Object get() {
        if(chosen) {
            return best;
        }

        Map allGivens = model.combinedGivens(givens);

        List<Double> scores = model.score(variants, allGivens);

        if(variants != null && variants.size() > 0) {
            DecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                if(ModelUtils.shouldtrackRunnersUp(variants.size(), tracker.getMaxRunnersUp())) {
                    // the more variants there are, the less frequently this is called
                    List<Object> rankedVariants = DecisionModel.rank(variants, scores);
                    best = rankedVariants.get(0);
                    id = tracker.track(best, variants, allGivens, model.getModelName(), true);
                } else {
                    // faster and more common path, avoids array sort
                    best = ModelUtils.topScoringVariant(variants, scores);
                    id = tracker.track(best, variants, allGivens, model.getModelName(), false);
                }
            } else {
                best = ModelUtils.topScoringVariant(variants, scores);
                IMPLog.e(Tag, "tracker not set on DecisionModel, decision will not be tracked");
            }
        } else {
            throw new IllegalStateException("variants to choose from can't be null or empty");
        }

        chosen = true;

        return best;
    }

    /**
     * Adds the reward to a specific decision
     * @param reward the reward to add. Must not be NaN, positive infinity, or negative infinity
     * @throws IllegalArgumentException Thrown if `reward` is NaN or +-Infinity
     * @throws IllegalStateException Thrown if the trackURL of underlying DecisionModel is null
     * The id could be null when addReward() is called prior to get(), or less likely the system
     * clock is so biased(beyond 2014~2150) that we can't generate a valid id(ksuid) when get()
     * is called.
     * */
    public void addReward(double reward) {
        if(id == null) {
            throw new IllegalStateException("trackURL can't be null when calling addReward()");
        }

        model.addRewardForDecision(id, reward);
    }
}
