package ai.improve;

import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

public class Decision<T> {
    /** @hidden */
    public static final String Tag = "Decision";

    private final DecisionModel model;

    /**
     * The id that uniquely identifies the decision after it's been tracked. It's null until
     * the decision is tracked by calling track().
     */
    private String id = null;

    /**
     * Additional context info that was used to score each of the variants.
     *  It's also included in tracking.
     */
    public final Map<String, ?> givens;

    /** The ranked variants */
    public final List<T> ranked;

    /** The best variant. Could be null if the variants list contains null members. */
    public final T best;

    protected Decision(DecisionModel model, List<T> rankedVariants, Map<String, ?> givens) {
        this.model = model;
        this.ranked = rankedVariants;
        this.givens = givens;
        this.best = rankedVariants.get(0);
    }

    /**
     * Getter of id.
     * @return Returns id.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the best variant.
     * @return Returns the best variant. Could be null if the variants list contains null members.
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public T peek() {
        return best;
    }

    /**
     * Gets the best variant, and also track the decision if it's not been tracked yet.
     * @return Returns the best variant. Could be null if the variants list contains null members.
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public synchronized T get() {
        if(id == null) {
            DecisionTracker tracker = model.getTracker();
            if(tracker == null) {
                IMPLog.w(Tag, "trackURL not set for the underlying DecisionModel. The decision " +
                        "won't be tracked.");
            } else {
                id = tracker.track(ranked, givens, model.getModelName());
            }
        }
        return best;
    }

    /**
     * Tracks the decision.
     * @return Returns the id that uniquely identifies the tracked decision.
     * @throws IllegalStateException Thrown if trackURL of the underlying DecisionModel is null;
     * Thrown if the decision is already tracked.
     */
    public synchronized String track() {
        if(id != null) {
            throw new IllegalStateException("the decision is already tracked!");
        }

        DecisionTracker tracker = model.getTracker();
        if(tracker == null) {
            throw new IllegalStateException("trackURL not set for the underlying DecisionModel!");
        }

        id = tracker.track(ranked, givens, model.getModelName());

        return id;
    }

    /**
     * For which(), whichFrom, and optimize().
     * @hidden
     */
    protected void track(DecisionTracker tracker) {
        if(tracker != null) {
            tracker.track(ranked, givens, model.getModelName());
        }
    }

    /**
     * Adds a reward that only applies to this specific decision. Before calling this method, make
     * sure that the decision is tracked by calling track().
     * @param reward the reward to add. Must not be NaN, positive infinity, or negative infinity
     * @throws IllegalArgumentException Thrown if `reward` is NaN or +-Infinity
     * @throws IllegalStateException Thrown if the trackURL of the underlying DecisionModel is null;
     * Thrown if the decision is not tracked yet.
     */
    public void addReward(double reward) {
        if(id == null) {
            throw new IllegalStateException("addReward() can't be called before track().");
        }
        model.addReward(reward, id);
    }
}
