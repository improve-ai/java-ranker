package ai.improve;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.improve.ksuid.KsuidGenerator;
import ai.improve.log.IMPLog;
import ai.improve.provider.PersistenceProvider;
import ai.improve.util.HttpUtil;
import ai.improve.util.Utils;

class DecisionTracker {
    public static final String Tag = "DecisionTracker";

    private static final String TYPE_KEY = "type";
    private static final String DECISION_TYPE = "decision";
    private static final String REWARD_TYPE = "reward";

    private static final String MODEL_KEY = "model";
    public static final String DECISION_BEST_KEY = "variant";
    public static final String COUNT_KEY = "count";
    private static final String GIVENS_KEY = "givens";
    private static final String RUNNERS_UP_KEY = "runners_up";
    public static final String SAMPLE_VARIANT_KEY = "sample";
    private static final String MESSAGE_ID_KEY = "message_id";
    private static final String DECISION_ID_KEY = "decision_id";
    private static final String REWARD_KEY = "reward";


    private static final String TRACK_API_KEY_HEADER = "x-api-key";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private static final int DEFAULT_MAX_RUNNERS_UP = 50;

    private final URL trackURL;

    private String trackApiKey;

    protected static PersistenceProvider persistenceProvider;

    private static final KsuidGenerator KSUID_GENERATOR = new KsuidGenerator();

    /**
     * Hyperparameter that affects training speed and model performance.
     * Values from 10-100 are probably reasonable.  0 disables runners up tracking
     * */
    private int maxRunnersUp = DEFAULT_MAX_RUNNERS_UP;

    public DecisionTracker(URL trackURL, String trackApiKey) {
        this.trackURL = trackURL;
        this.trackApiKey = trackApiKey;
    }

    protected static void setPersistenceProvider(PersistenceProvider persistenceProvider) {
        DecisionTracker.persistenceProvider = persistenceProvider;
    }

    public int getMaxRunnersUp() {
        return maxRunnersUp;
    }

    /**
     * @param maxRunnersUp  Hyperparameter that affects training speed and model performance.
     *                      Values from 10-100 are probably reasonable.
     *                      0 disables runners up tracking
     * */
    public void setMaxRunnersUp(int maxRunnersUp) {
        this.maxRunnersUp = Math.max(maxRunnersUp, 0);
    }

    protected String getTrackApiKey() {
        return trackApiKey;
    }

    protected void setTrackApiKey(String trackApiKey) {
        this.trackApiKey = trackApiKey;
    }

    /**
     * Decision.get() throws an error if variants is empty or null. So it's safe to assume here
     * that bestVariant is not null , and variants.size() > 0
     * @return the message_id of the tracked decision; null is returned in case of errors
     * */
    protected <T> String track(List<T> rankedVariants, Map<String, ?> givens, String modelName) {
        if(modelName == null || modelName.isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: modelName is empty or null");
            return null;
        }

        String decisionId = createAndPersistDecisionIdForModel(modelName);

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(MODEL_KEY, modelName);
        body.put(MESSAGE_ID_KEY, decisionId);

        setCount(rankedVariants, body);

        setBestVariant(rankedVariants.get(0), body);

        if(givens != null) {
            body.put(GIVENS_KEY, givens);
        }

        List<T> runnersUp = null;
        boolean shouldTrackRunnersUp = shouldTrackRunnersUp(rankedVariants.size(), maxRunnersUp);
        if(shouldTrackRunnersUp) {
            runnersUp = topRunnersUp(rankedVariants, maxRunnersUp);
            body.put(RUNNERS_UP_KEY, runnersUp);
        }

        int runnersUpCount = runnersUp == null ? 0 : runnersUp.size();
        setSampleVariant(rankedVariants, runnersUpCount, body);

        postTrackingRequest(body);

        return decisionId;
    }

    protected void setBestVariant(Object variant, Map<String, Object> body) {
        body.put(DECISION_BEST_KEY, variant);
    }

    protected <T> void setCount(List<T> variants, Map<String, Object> body) {
        body.put(COUNT_KEY, variants.size());
    }

    protected <T> List<T> topRunnersUp(List<T> ranked, int maxRunnersUp) {
        return ranked.subList(1, 1+Math.min(maxRunnersUp, ranked.size()-1));
    }

    /**
     * Sample variant is selected from variants excluding runners-up and
     * the best variant
     *
     * If there are no runners up, then sample is a random sample from
     * variants with just best excluded.
     *
     * If there are runners up, then sample is a random sample from
     * variants with best and runners up excluded.
     *
     * If there is only one variant, which is the best, then there is no sample.
     *
     * If there are no remaining variants after best and runners up, then
     * there is no sample.
     *
     * If the sample variant itself is null, it should also be included in the body map.
     **/
    protected <T> void setSampleVariant(List<T> rankedVariants, int runnersUpCount, Map<String, Object> body) {
        if(rankedVariants == null || rankedVariants.size() <= 0) {
            return ;
        }

        int samplesCount = rankedVariants.size() - runnersUpCount - 1;
        if (samplesCount <= 0) {
            return ;
        }

        int randomIndex = new Random().nextInt(samplesCount) + runnersUpCount + 1;
        body.put(SAMPLE_VARIANT_KEY, rankedVariants.get(randomIndex));
    }

    /**
     * Adds reward for the last decision of a model
     * */
    public void addRewardForModel(String modelName, double reward) {
        String lastDecisionId = lastDecisionIdOfModel(modelName);
        if(Utils.isEmpty(lastDecisionId)) {
            IMPLog.w(Tag, "add reward for [" + modelName + "], but lastDecisionId is empty");
            return ;
        }

        addRewardForDecision(modelName, lastDecisionId, reward);
    }

    /**
     * Adds reward for a specific decision of a model
     * */
    public void addRewardForDecision(String modelName, String decisionId, double reward) {
        String ksuid = KSUID_GENERATOR.next();
        if(ksuid == null) {
            IMPLog.w(Tag, "failed to generate ksuid");
            return ;
        }

        Map<String, Object> body = getAddDecisionRewardRequestBody(ksuid, modelName, decisionId, reward);

        postTrackingRequest(body);

        if(persistenceProvider != null) {
            persistenceProvider.addRewardForModel(modelName, reward);
        }
    }

    protected Map<String, Object> getAddDecisionRewardRequestBody(String ksuid, String modelName, String decisionId, double reward) {
        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, REWARD_TYPE);
        body.put(MODEL_KEY, modelName);
        body.put(DECISION_ID_KEY, decisionId);
        body.put(MESSAGE_ID_KEY, ksuid);
        body.put(REWARD_KEY, reward);
        return body;
    }

    private void postTrackingRequest(Map<String, Object> body) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        if(trackApiKey != null) {
            headers.put(TRACK_API_KEY_HEADER, trackApiKey);
        }
        HttpUtil.withUrl(trackURL).withHeaders(headers).withBody(body).post();
    }

    private String createAndPersistDecisionIdForModel(String modelName) {
        String decisionId = KSUID_GENERATOR.next();
        if(decisionId != null && persistenceProvider != null) {
            persistenceProvider.persistDecisionIdForModel(modelName, decisionId);
        }
        return decisionId;
    }

    protected String lastDecisionIdOfModel(String modelName) {
        return persistenceProvider.lastDecisionIdForModel(modelName);
    }

    protected static boolean shouldTrackRunnersUp(int variantsCount, int maxRunnersUp) {
        if(variantsCount <= 1 || maxRunnersUp == 0) {
            return false;
        }
        return Math.random() < 1.0 / Math.min(variantsCount - 1, maxRunnersUp);
    }
}
