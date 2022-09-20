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
     * Get the chosen variant.
     * @return Returns the chosen variant. Could be null if the variants list contains null members.
     * */
    public T get() {
        return rankedVariants.get(0);
    }

    /**
     * Get the ranked variants.
     */
    public List<T> ranked() {
        return rankedVariants;
    }

    /**
     * Tracks the decision.
     * @return Returns the id that uniquely identifies the tracked decision. It might be null when
     * the system clock is beyond year 2014~2150 and the SDK fails to generate a valid id(ksuid).
     */
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
     * Adds a reward that only applies to this specific decision. Before calling this method, make
     * sure that track() is called and a nonnull id that identifies the tracked decision is returned.
     * @param reward the reward to add. Must not be NaN, positive infinity, or negative infinity
     * @throws IllegalArgumentException Thrown if `reward` is NaN or +-Infinity
     * @throws IllegalStateException Thrown if the trackURL of the underlying DecisionModel is null;
     * Thrown if track() is not called, or called but the returned id is null.
     * */
    public void addReward(double reward) {
        if(id == null) {
            throw new IllegalStateException("addReward() can't be called before track().");
        }
        model.addReward(reward, id);
    }
}
