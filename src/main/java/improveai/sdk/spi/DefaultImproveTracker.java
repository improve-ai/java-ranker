package improveai.sdk.spi;

import improveai.sdk.ImproveTrackCompletion;
import improveai.sdk.ImproveTracker;

import java.util.List;
import java.util.Map;

public class DefaultImproveTracker implements ImproveTracker {

    private String trackUrl;

    private String apiKey;

    public DefaultImproveTracker(String trackUrl) {
        this.trackUrl = trackUrl;
    }

    public DefaultImproveTracker(String trackUrl, String apiKey) {
        this.trackUrl = trackUrl;
        this.apiKey = apiKey;
    }


    @Override
    public void trackDecision(Object variant, List variants, String modelName) {
        trackDecision(variant, variants, modelName, null, null, null);
    }

    @Override
    public void trackDecision(Object variant, List variants, String modelName, Map context) {
        trackDecision(variant, variants, modelName, context, null, null);

    }

    @Override
    public void trackDecision(Object variant, List variants, String modelName, Map context, String rewardKey) {
        trackDecision(variant, variants, modelName, context, rewardKey, null);

    }

    @Override
    public void trackDecision(Object variant, List variants, String modelName, Map context, String rewardKey, ImproveTrackCompletion completionHandler) {
        //do nothing yet
    }

    @Override
    public void addReward(Double reward, String rewardKey) {

    }

    @Override
    public void addRewards(Map<String, Double> rewards) {

    }

    @Override
    public void addRewards(Map<String, Double> rewards, ImproveTrackCompletion completionHandler) {

    }
}
