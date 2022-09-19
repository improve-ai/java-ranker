package ai.improve;

import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

public class Decision<T> {
    public static final String Tag = "Decision";

    private final DecisionModel model;

    protected Map<String, ?> givens;

    protected List<T> rankedVariants;

    /**
     * A decision should be tracked only once no matter how many times get()/ranked() is called.
     * A boolean here may be more appropriate in the first glance. But I find it hard to unit test
     * that it's tracked only once with a boolean value in multi-thread mode. So I'm
     * using an int here with 0 as 'untracked', and anything else as 'tracked'.
     * */
    protected int tracked = 0;

    // The message_id of the tracked decision
    protected String id;

    protected Decision(DecisionModel model, List<T> rankedVariants, Map<String, ?> givens) {
        this.model = model;
        this.rankedVariants = rankedVariants;
        this.givens = givens;
    }

    public T get() {
        return get(true);
    }

    /**
     * Get the chosen variant and track the decision. The decision would be tracked only once.
     * @return Returns the chosen variant memoized. When the variants contains null members, get()
     * might return null.
     * @throws IllegalStateException Thrown if variants is null or empty.
     * */
    public T get(boolean trackOnce) {
        if(trackOnce) {
            trackOnce();
        }
        return rankedVariants.get(0);
    }

    public List<T> ranked() {
        return ranked(true);
    }

    public List<T> ranked(boolean trackOnce) {
        if(trackOnce) {
            trackOnce();
        }
        return rankedVariants;
    }

    /**
     * Adds a reward that only applies to this specific decision. This method should not be called
     * prior to get() or ranked().
     * @param reward the reward to add. Must not be NaN, positive infinity, or negative infinity
     * @throws IllegalArgumentException Thrown if `reward` is NaN or +-Infinity
     * @throws IllegalStateException Thrown if the trackURL of underlying DecisionModel is null
     * The id could be null when addReward() is called prior to get()/ranked(), or less likely the system
     * clock is so biased(beyond 2014~2150) that we can't generate a valid id(ksuid) when get()/ranked()
     * is called.
     * */
    public void addReward(double reward) {
        if(id == null) {
            throw new IllegalStateException("id can't be null. Make sure that addReward() is " +
                    "called after get(); and the trackURL is set in the DecisionModel.");
        }
        model.addReward(reward, id);
    }

    private synchronized void trackOnce() {
        if(tracked == 0) {
            DecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                id = tracker.track(rankedVariants, givens, model.getModelName());
                tracked++;
            } else {
                IMPLog.e(Tag, "tracker not set on DecisionModel, decision will not be tracked");
            }
        }
    }
}
