package improveai.sdk;

import java.util.List;
import java.util.Map;

public interface ImproveModel {


    /**
     * Chooses a variant that is expected to maximize future rewards. Call `-trackDecision:` and
     * `-addReward:` in order to train the model after choosing.
     * <p>
     *
     * @param variants A JSON encodeable list of variants to choose from.
     *
     * @return The chosen variant, which may be different between calls even with the same inputs. If model is not
     * ready, immediately returns the first variant.
     */
    Object choose(List variants);

    /**
     * Chooses a variant that is expected to maximize future rewards for the given context. Call `-trackDecision:` and
     * `-addReward:` in order to train the model after choosing.
     *
     * @param variants A JSON encodeable list of variants to choose from.
     * @param context  A JSON encodeable dictionary of key value pairs that describe the context that choose should be
     *                 optimized for.
     * @return The chosen variant, which may be different between calls even with the same inputs.  If model is not
     * ready, immediately returns the first variant.
     */
    Object choose(List<Object> variants, Map<String, Object> context);

    /**
     * Sorts variants from largest to smallest expected future rewards.
     *
     * @param variants A JSON encodeable list of variants to sort.
     * @return A sorted copy of the variants array from largest to smallest expected future rewards, which may be
     * different between calls even with the same inputs.  If model is not ready, immediately returns a shallow
     * unsorted copy of the variants.
     */
    List sort(List variants);

    /**
     * Sorts variants from largest to smallest expected future rewards for the given context.  None of the variants
     * will be tracked, so no learning will take place unless trackChosen, or choose with autoTrack enabled, are called.
     *
     * @param variants A JSON encodeable list of variants to sort.
     * @param context  A JSON encodeable dictionary of key value pairs that describe the context that sort should be
     *                 optimized for.
     * @return A sorted copy of the variants array from largest to smallest expected future rewards, which may be different between calls even with the same inputs.  If model is not ready, immediately returns a shallow unsorted copy of the variants.
     */
    List sort(List variants, Map context);


    /**
     * Takes an array of variants and returns an List of Numbers with the scores.
     */
    List<Number> score(List variants);

    /**
     * Takes an array of variants and context and returns an array of NSNumbers of the scores.
     */
    List<Number> score(List variants, Map context);


}
