package improveai.sdk;

import java.util.List;
import java.util.Map;

public interface ImproveModel {


    /**
     * Chooses a variant that is expected to maximize future rewards. Call `-trackDecision:` and
     * `-addReward:` in order to train the model after choosing.
     *
     * TODO review with the customer what is expected in variants collection
     * @param variants A JSON encodeable list of variants to choose from.  May contain values of type NSDictionary,
     *                 NSArray, NSString, NSNumber, and NSNull.  NSDictionary keys must be of type NSString.
     *                 NaN and infinity values are not allowed for NSNumber because they are not JSON encodable.
     *
     * @return The chosen variant, which may be different between calls even with the same inputs. If model is not
     * ready, immediately returns the first variant.
     */
    Object choose(List variants);

    /**
     * Chooses a variant that is expected to maximize future rewards for the given context. Call `-trackDecision:` and
     * `-addReward:` in order to train the model after choosing.
     *
     * @param variants A JSON encodeable list of variants to choose from.  May contain values of type NSDictionary,
     *                 NSArray, NSString, NSNumber, and NSNull.  NSDictionary keys must be of type NSString. NaN and
     *                 infinity values are not allowed for NSNumber because they are not JSON encodable.
     * @param context  A JSON encodeable dictionary of key value pairs that describe the context that choose should be
     *                 optimized for. May contain values of type NSDictionary, NSArray, NSString, NSNumber, and NSNull.
     *                 NSDictionary keys must be of type NSString. NaN and infinity values are not allowed for NSNumber
     *                 because they are not JSON encodable.
     *
     * @return The chosen variant, which may be different between calls even with the same inputs.  If model is not
     * ready, immediately returns the first variant.
     */
    Object choose(List<Object> variants, Map<String, Object> context);

    /**
     * Sorts variants from largest to smallest expected future rewards.
     *
     * @param variants A JSON encodeable list of variants to sort.  May contain values of type NSDictionary, NSArray,
     *                 NSString, NSNumber, and NSNull.  NSDictionary keys must be of type NSString. NaN and infinity
     *                 values are not allowed for NSNumber because they are not JSON encodable.
     *
     * @return A sorted copy of the variants array from largest to smallest expected future rewards, which may be
     * different between calls even with the same inputs.  If model is not ready, immediately returns a shallow
     * unsorted copy of the variants.
     */
    List sort(List variants);

    /**
     * Sorts variants from largest to smallest expected future rewards for the given context.  None of the variants
     * will be tracked, so no learning will take place unless trackChosen, or choose with autoTrack enabled, are called.
     *
     * @param variants A JSON encodeable list of variants to sort.  May contain values of type NSDictionary, NSArray,
     *                 NSString, NSNumber, and NSNull.  NSDictionary keys must be of type NSString. NaN and infinity
     *                 values are not allowed for NSNumber because they are not JSON encodable.
     * @param context  A JSON encodeable dictionary of key value pairs that describe the context that sort should be
     *                 optimized for. May contain values of type NSDictionary, NSArray, NSString, NSNumber, and NSNull.
     *                 NSDictionary keys must be of type NSString. NaN and infinity values are not allowed for NSNumber
     *                 because they are not JSON encodable.
     * @return A sorted copy of the variants array from largest to smallest expected future rewards, which may be different between calls even with the same inputs.  If model is not ready, immediately returns a shallow unsorted copy of the variants.
     */
    List sort(List variants, Map context);


}
