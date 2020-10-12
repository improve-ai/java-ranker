package improveai.sdk;

import java.util.List;
import java.util.Map;

public interface ImproveTracker {

    /**
     * Track that a variant was chosen in order to train the system to learn what rewards it receives.
     *
     * @param variant The JSON encodeable chosen variant to track
     */
    void trackDecision(Object variant, List variants, String modelName);

    /**
     * Track that a variant was chosen in order to train the system to learn what rewards it receives.
     *
     * @param variant The JSON encodeable chosen variant to track
     * @param context The JSON encodeable context that the chosen variant is being used in and should be rewarded against.
     *                It is okay for this to be different from the context that was used during choose or sort.
     */
    void trackDecision(Object variant, List variants, String modelName, Map context);


    /**
     * Track that a variant was chosen in order to train the system to learn what rewards it receives.
     *
     * @param variant   The JSON encodeable chosen variant to track
     * @param context   The JSON encodeable context that the chosen variant is being used in and should be rewarded against.
     *                  It is okay for this to be different from the context that was used during choose or sort.
     * @param rewardKey The rewardKey used to assign rewards to the chosen variant. If nil, rewardKey is set to the namespace.
     *                  trackRewards must also use this key to assign rewards to this chosen variant.
     */
    void trackDecision(Object variant, List variants, String modelName, Map context, String rewardKey);

    /**
     * Track that a variant was chosen in order to train the system to learn what rewards it receives.
     *
     * @param variant           The JSON encodeable chosen variant to track
     * @param context           The JSON encodeable context that the chosen variant is being used in and should be rewarded against.
     *                          It is okay for this to be different from the context that was used during choose or sort.
     * @param rewardKey         The rewardKey used to assign rewards to the chosen variant. If nil, rewardKey is set to the namespace.
     *                          trackRewards must also use this key to assign rewards to this chosen variant.
     * @param completionHandler Called after sending the decision to the server.
     */
    void trackDecision(Object variant, List variants, String modelName, Map context, String rewardKey, ImproveTrackCompletion completionHandler);


    /**
     * Tracks a reward value for one or more chosen variants. Rewards are additive by default. Multiple chosen variants
     * can be listening for the same reward key
     *
     * @param reward    a JSON encodeable reward vaue to add to recent chosen variants for rewardKey.
     *                  May be a negative number.  Must not be NaN or infinity.
     * @param rewardKey the model name or custom rewardKey to track this reward for.
     */
    void addReward(Double reward, String rewardKey);

    /**
     * Tracks rewards for one or more chosen variants. Rewards are additive by default.  Multiple chosen variants can
     * be listening for the same reward key.
     *
     * @param rewards a JSON encodeable dictionary mapping rewardKeys to reward values to add to recent chosen variants.
     *                Reward values may be negative numbers, must not be NaN or infinity.
     */
    void addRewards(Map<String, Double> rewards);

    /**
     * Tracks rewards for one or more chosen variants. Rewards are additive by default.  Multiple chosen variants can be
     * listening for the same reward key.
     *
     * @param rewards           a JSON encodeable dictionary mapping rewardKeys to reward values to add to recent chosen
     *                          variants.  Reward values may be negative numbers, must not be NaN or infinity.
     * @param completionHandler Called after sending the rewards.
     */
    void addRewards(Map<String, Double> rewards, ImproveTrackCompletion completionHandler);


    /**
     * Tracks a general analytics event that may be further processed by backend scripts.  You may use this for example
     * to keep reward assignment logic on the backend.  In the case where all reward logic is handled on the backend
     * you may wish to disable autoTrack on choose calls and not call trackRewards.
     *
     * @param event      the name of the event to track
     * @param properties JSON encodable event properties
     */
    void trackAnalyticsEvent(String event, Map<String, Object> properties);

    /**
     * Tracks a general analytics event that may be further processed by backend scripts.  You may use this for example
     * to keep reward assignment logic on the backend.  In the case where all reward logic is handled on the backend
     * you may wish to disable autoTrack on choose calls and not call trackRewards.
     *
     * @param event      the name of the event to track
     * @param properties JSON encodable event properties
     * @param context    JSON encodeable context
     */
    void trackAnalyticsEvent(String event, Map<String, Object> properties, Map<String, Object> context);

}
