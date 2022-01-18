package ai.improve;

import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;
import ai.improve.util.ModelUtils;

public class Decision {
    public static final String Tag = "Decision";

    private DecisionModel model;

    protected List<?> variants;

    protected Map<String, Object> givens;

    protected Map<String, Object> allGivens;

    protected List<Double> scores;

    /**
     * A decision should be tracked only once when calling get(). A boolean here may
     * be more appropriate in the first glance. But I find it hard to unit test
     * that it's tracked only once with a boolean value in multi-thread mode. So I'm
     * using an int here with 0 as 'untracked', and anything else as 'tracked'.
     * */
    protected int tracked;

    protected Object best;

    // The message_id of the tracked decision
    private String id;

    protected Decision(DecisionModel model) {
        this.model = model;
    }

    /**
     * Same as get() except that peek won't track the decision.
     * @return Returns the chosen variant memoized.
     * @throws IllegalStateException Thrown if called before chooseFrom()
     */
    public Object peek() {
        return best;
    }

    /**
     * Get the chosen variant and track the decision. The decision would be tracked only once.
     * @return Returns the chosen variant memoized.
     * @throws IllegalStateException Thrown if variants is null or empty.
     * */
    public synchronized Object get() {
        if(tracked == 0) {
            DecisionTracker tracker = model.getTracker();
            if (tracker != null) {
                if (ModelUtils.shouldtrackRunnersUp(variants.size(), tracker.getMaxRunnersUp())) {
                    // the more variants there are, the less frequently this is called
                    List<Object> rankedVariants = DecisionModel.rank(variants, scores);
                    id = tracker.track(best, rankedVariants, allGivens, model.getModelName(), true);
                } else {
                    // faster and more common path, avoids array sort
                    id = tracker.track(best, variants, allGivens, model.getModelName(), false);
                }
                tracked++;
            } else {
                IMPLog.e(Tag, "tracker not set on DecisionModel, decision will not be tracked");
            }
        }
        return best;
    }

    /**
     * Adds a reward that only applies to this specific decision. This method should not be called
     * prior to get().
     * @param reward the reward to add. Must not be NaN, positive infinity, or negative infinity
     * @throws IllegalArgumentException Thrown if `reward` is NaN or +-Infinity
     * @throws IllegalStateException Thrown if the trackURL of underlying DecisionModel is null
     * The id could be null when addReward() is called prior to get(), or less likely the system
     * clock is so biased(beyond 2014~2150) that we can't generate a valid id(ksuid) when get()
     * is called.
     * */
    public void addReward(double reward) {
        if(id == null) {
            throw new IllegalStateException("id can't be null. Make sure that addReward() is " +
                    "called after get(); and the trackURL is set in the DecisionModel.");
        }
        model.addRewardForDecision(id, reward);
    }
}
