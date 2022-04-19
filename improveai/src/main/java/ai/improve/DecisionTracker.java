package ai.improve;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String MESSAGE_ID_KEY = "message_id";
    private static final String DECISION_ID_KEY = "decision_id";
    private static final String REWARD_KEY = "reward";


    private static final String TRACK_API_KEY_HEADER = "x-api-key";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private static final SimpleDateFormat ISO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);

    private static final int DEFAULT_MAX_RUNNERS_UP = 50;

    private String trackURL;

    private String trackApiKey;

    protected static PersistenceProvider persistenceProvider;

    private static final KsuidGenerator KSUID_GENERATOR = new KsuidGenerator();

    /**
     * Hyperparameter that affects training speed and model performance.
     * Values from 10-100 are probably reasonable.  0 disables runners up tracking
     * */
    private int maxRunnersUp = DEFAULT_MAX_RUNNERS_UP;

    public DecisionTracker(String trackURL, String trackApiKey) {
        this.trackURL = trackURL;
        this.trackApiKey = trackApiKey;
        if(Utils.isEmpty(trackURL)) {
            // Just give a warning
            IMPLog.e(Tag, "trackURL is empty or null, tracking disabled");
        }
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
        this.maxRunnersUp = maxRunnersUp >= 0 ? maxRunnersUp : 0;
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
    protected <T> String track(Object bestVariant, List<T> variants, Map<String, Object> givens,
                                 String modelName, boolean variantsRankedAndTrackRunnersUp) {
        if(modelName == null || modelName.isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: modelName is empty or null");
            return null;
        }

        if(Utils.isEmpty(trackURL)) {
            IMPLog.e(Tag, "Improve.track error: trackURL is empty or null");
            return null;
        }

        String decisionId = createAndPersistDecisionIdForModel(modelName);
        if(decisionId == null) {
            IMPLog.e(Tag, "decisionId generated is null");
            return null;
        }

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(MODEL_KEY, modelName);
        body.put(MESSAGE_ID_KEY, decisionId);

        setCount(variants, body);

        setBestVariant(bestVariant, body);

        if(givens != null) {
            body.put(GIVENS_KEY, givens);
        }

        List<T> runnersUp = null;
        if(variantsRankedAndTrackRunnersUp) {
            runnersUp = topRunnersUp(variants, maxRunnersUp);
            body.put(RUNNERS_UP_KEY, runnersUp);
        }

        int runnersUpCount = runnersUp == null ? 0 : runnersUp.size();
        setSampleVariant(variants, runnersUpCount, variantsRankedAndTrackRunnersUp, bestVariant, body);

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
    protected <T> void setSampleVariant(List<T> variants, int runnersUpCount, boolean ranked, Object bestVariant, Map<String, Object> body) {
        if(variants == null || variants.size() <= 0) {
            return ;
        }

        int samplesCount = variants.size() - runnersUpCount - 1;
        if (samplesCount <= 0) {
            return ;
        }

        if(ranked) {
            int randomIndex = new Random().nextInt(samplesCount) + runnersUpCount + 1;
            body.put(SAMPLE_VARIANT_KEY, variants.get(randomIndex));
        } else {
            int indexOfBestVariant = variants.indexOf(bestVariant);
            Random r = new Random();
            while (true) {
                int randomIdx = r.nextInt(variants.size());
                if(randomIdx != indexOfBestVariant) {
                    body.put(SAMPLE_VARIANT_KEY, variants.get(randomIdx));
                    break;
                }
            }
        }
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

        persistenceProvider.addRewardForModel(modelName, reward);
    }

    protected Map getAddDecisionRewardRequestBody(String ksuid, String modelName, String decisionId, double reward) {
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

        body = new HashMap<>(body);
        body.put(TIMESTAMP_KEY, ISO_TIMESTAMP_FORMAT.format(new Date()));

        Map<String, Object> finalBody = body;

        // It's not allowed to send network request in the main thread on Android.
        new Thread() {
            @Override
            public void run() {
                try {
                    // android.os.NetworkOnMainThreadException will be thrown if post() is called
                    // in main thread
                    HttpUtil.withUrl(trackURL).withHeaders(headers).withBody(finalBody).post();
                } catch (MalformedURLException e) {
                    IMPLog.e(Tag, e.getLocalizedMessage());
                }
            }
        }.start();
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
}
