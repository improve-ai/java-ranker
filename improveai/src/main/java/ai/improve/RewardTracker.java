package ai.improve;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.improve.ksuid.KsuidGenerator;
import ai.improve.provider.PersistenceProvider;
import ai.improve.util.HttpUtil;
import ai.improve.util.Utils;

public class RewardTracker {
    private static final String TYPE_KEY = "type";
    private static final String DECISION_TYPE = "decision";
    private static final String REWARD_TYPE = "reward";

    private static final String MODEL_KEY = "model";
    public static final String ITEM_KEY = "item";
    public static final String COUNT_KEY = "count";
    private static final String CONTEXT_KEY = "context";
    public static final String SAMPLE_ITEM_KEY = "sample";
    private static final String MESSAGE_ID_KEY = "message_id";
    private static final String DECISION_ID_KEY = "decision_id";
    private static final String REWARD_KEY = "reward";

    private static final String TRACK_API_KEY_HEADER = "x-api-key";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private String modelName;

    private URL trackURL;

    private String trackApiKey;

    private static final KsuidGenerator KSUID_GENERATOR = new KsuidGenerator();

    public static PersistenceProvider persistenceProvider;

    /**
     * @param modelName Length of modelName must be in range [1, 64]; Only alphanumeric
     *                  characters([a-zA-Z0-9]), '-', '.' and '_' are allowed in the modelName
     *                  and the first character must be an alphanumeric one.
     * @param trackUrl The track endpoint URL that all tracked data will be sent to.
     * @param trackApiKey track endpoint API key (if applicable); can be null.
     */
    public RewardTracker(String modelName, URL trackUrl, String trackApiKey) {
        if(!Utils.isValidModelName(modelName)) {
            throw new IllegalArgumentException("invalid modelName: [" + modelName + "]");
        }

        if(trackUrl == null) {
            throw new IllegalArgumentException("trackUrl can't be null");
        }

        this.modelName = modelName;
        this.trackURL = trackUrl;
        this.trackApiKey = trackApiKey;
    }

    public String getModelName() {
        return this.modelName;
    }

    public URL getTrackURL() {
        return this.trackURL;
    }

    public String getTrackApiKey() {
        return this.trackApiKey;
    }

    public String track(Object item, List<?> candidates) {
        return track(item, candidates, null);
    }

    /**
     * Tracks the item selected from candidates and a random sample from the remaining ones.
     *
     * @param item Any JSON encodable object chosen from candidates.
     * @param candidates Collection of items from which the item is chosen.
     * @param context Extra context info that was used to score the candidates.
     * @return rewardId of this track request.
     * @throws IllegalArgumentException Thrown if candidates is null; Thrown if item is not included
     * in candidates; Thrown if item/context or a random sample picked from candidates is not JSON
     * encodable.
     */
    public String track(Object item, List<?> candidates, Object context) {
        if(candidates == null) {
            throw new IllegalArgumentException("candidates can't be null");
        }

        int index = candidates.indexOf(item);
        if(index == -1) {
            throw new IllegalArgumentException("candidates must include item!");
        }

        Object sample = null;
        if(candidates.size() > 1) {
            int randomIndex;
            Random random = new Random();
            while (true) {
                randomIndex = random.nextInt(candidates.size());
                if (randomIndex != index) {
                    break;
                }
            }
            sample = candidates.get(randomIndex);
        }

        String ksuid = KSUID_GENERATOR.next();

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(MODEL_KEY, modelName);
        body.put(COUNT_KEY, candidates.size());
        body.put(MESSAGE_ID_KEY, ksuid);

        body.put(ITEM_KEY, item);

        // Exclude sample if there's none.
        if(candidates.size() > 1) {
            body.put(SAMPLE_ITEM_KEY, sample);
        }

        // Exclude if context is null
        if(context != null) {
            body.put(CONTEXT_KEY, context);
        }

        if(!HttpUtil.isJsonEncodable(body)) {
            throw new IllegalArgumentException("item, context or a random sample picked from candidates is not JSON encodable!");
        }

        postTrackingRequest(body);

        return ksuid;
    }

    public String trackWithSample(Object item, Object sample, int numCandidates) {
        return trackWithSample(item, sample, numCandidates, null);
    }

    /**
     * Tracks the item selected from candidates and a specific sample from the remaining ones.
     *
     * @param item The selected item.
     * @param sample A random sample from the candidates. If there's no sample, please call the track()
     *               method instead.
     * @param numCandidates total number of candidates, including the selected item.
     * @param context Extra context info that was used to score these candidates.
     * @return rewardId of this track request.
     * @throws IllegalArgumentException Thrown if numCandidates < 2; Thrown if item/sample/context
     * is not JSON encodable.
     */
    public String trackWithSample(Object item, Object sample, int numCandidates, Object context) {
        if(numCandidates < 2) {
            throw new IllegalArgumentException("numCandidates can't be smaller than 2");
        }

        String ksuid = KSUID_GENERATOR.next();

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(MODEL_KEY, modelName);
        body.put(COUNT_KEY, numCandidates);
        body.put(MESSAGE_ID_KEY, ksuid);

        body.put(ITEM_KEY, item);

        body.put(SAMPLE_ITEM_KEY, sample);

        // exclude is context is null
        if(context != null) {
            body.put(CONTEXT_KEY, context);
        }

        if(!HttpUtil.isJsonEncodable(body)) {
            throw new IllegalArgumentException("item/sample/context must be JSON encodable!");
        }

        postTrackingRequest(body);

        return ksuid;
    }

    /**
     * Adds reward for a previous decision tracked by rewardId
     *
     * @param reward The reward to add. Must not be NaN or infinity.
     * @param rewardId The id that was returned from track() methods.
     */
    public void addReward(double reward, String rewardId) {
        if(Double.isInfinite(reward) || Double.isNaN(reward)) {
            throw new IllegalArgumentException("reward must not be NaN or infinity");
        }

        if(rewardId == null || rewardId.length() != KsuidGenerator.KSUID_STRING_LENGTH) {
            throw new IllegalArgumentException("invalid rewardId. Please use the one returned from track().");
        }

        String ksuid = KSUID_GENERATOR.next();

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, REWARD_TYPE);
        body.put(MODEL_KEY, modelName);
        body.put(MESSAGE_ID_KEY, ksuid);
        body.put(DECISION_ID_KEY, rewardId);
        body.put(REWARD_KEY, reward);

        postTrackingRequest(body);
    }

    private void postTrackingRequest(Map<String, Object> body) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        if(trackApiKey != null) {
            headers.put(TRACK_API_KEY_HEADER, trackApiKey);
        }
        HttpUtil.withUrl(trackURL).withHeaders(headers).withBody(body).post();
    }

    protected static void setPersistenceProvider(PersistenceProvider persistenceProvider) {
        RewardTracker.persistenceProvider = persistenceProvider;
    }
}
