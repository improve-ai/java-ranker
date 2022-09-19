package ai.improve;

import java.util.List;
import java.util.Map;

public class Decision<T> {
    public static final String Tag = "Decision";

    private final DecisionModel model;

    protected Map<String, ?> givens;

    protected List<T> rankedVariants;

    // The message_id of the tracked decision
    protected String id;

    protected Decision(DecisionModel model, List<T> rankedVariants, Map<String, ?> givens) {
        this.model = model;
        this.rankedVariants = rankedVariants;
        this.givens = givens;
    }

    /**
     * Get the chosen variant and track the decision. The decision would be tracked only once.
     * @return Returns the chosen variant. Could be null if the variants list contains null members.
     * */
    public T get() {
        return rankedVariants.get(0);
    }

    public List<T> ranked() {
        return rankedVariants;
    }

    public synchronized String track() {
        if(id != null) {
            throw new IllegalStateException("the decision is already tracked!");
        }

        DecisionTracker tracker = model.getTracker();
        if(tracker == null) {
            throw new IllegalStateException("trackURL not set for the underlying DecisionModel!");
        }

        id = tracker.track(rankedVariants, givens, model.getModelName());

        return id;
    }

    /**
     * Adds a reward that only applies to this specific decision. This method should not be called
     * prior to track().
     * @param reward the reward to add. Must not be NaN, positive infinity, or negative infinity
     * @throws IllegalArgumentException Thrown if `reward` is NaN or +-Infinity
     * @throws IllegalStateException Thrown if the trackURL of the underlying DecisionModel is null;
     * Thrown if {@link #id} is null. The id could be null when addReward() is called prior to track(),
     * or less likely the system clock is so biased(beyond 2014~2150) that we can't generate a
     * valid id(ksuid) in track().
     * */
    public void addReward(double reward) {
        if(id == null) {
            throw new IllegalStateException("addReward() can't be called before track().");
        }
        model.addReward(reward, id);
    }
}
