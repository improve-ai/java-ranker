package ai.improve;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import ai.improve.log.IMPLog;
import ai.improve.util.HttpUtil;
import ai.improve.util.Utils;

public class DecisionTracker {
    public static final String Tag = "BaseDecisionTracker";

    private static final String TYPE_KEY = "type";

    private static final String DECISION_TYPE = "decision";
    private static final String MODEL_KEY = "model";
    public static final String DECISION_BEST_KEY = "variant";
    public static final String COUNT_KEY = "count";
    private static final String GIVENS_KEY = "givens";
    private static final String RUNNERS_UP_KEY = "runners_up";
    public static final String SAMPLE_VARIANT_KEY = "sample";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String MESSAGE_ID_KEY = "message_id";
    private static final String PROPERTIES_KEY = "properties";
    private static final String EVENT_KEY = "event";


    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private static final SimpleDateFormat ISO_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);


    private static final String HISTORY_ID_KEY = "history_id";

    private static final int DEFAULT_MAX_RUNNERS_UP = 50;

    private static String sHistoryId = "";

    private String trackURL;

    /**
     * Hyperparameter that affects training speed and model performance.
     * Values from 10-100 are probably reasonable.  0 disables runners up tracking
     * */
    private int maxRunnersUp = DEFAULT_MAX_RUNNERS_UP;

    /**
     * Android only
     * */
    public DecisionTracker(String trackURL) {
        this(trackURL, null);
    }

    /**
     * History id must be set for non-Android platforms, so this is the only
     * valid constructor method for them.
     * */
    public DecisionTracker(String trackURL, String historyId) {
        this.trackURL = trackURL;

        if(trackURL == null || trackURL.isEmpty()) {
            // Just a warning
            IMPLog.e(Tag, "trackURL is empty or null, tracking disabled");
        }

        if(Utils.isAndroid()) {
            String id = getHistoryId();
            if(Utils.isEmpty(id)) {
                throw new RuntimeException("Fatal error, history id must not be null or empty");
            }
            setHistoryId(id);
        } else {
            // history id must be set for non-Android platform
            if(Utils.isEmpty(historyId)) {
                throw new RuntimeException("Fatal error, history id must not be null or empty");
            }
            sHistoryId = historyId;
        }
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

    protected <T> void track(Object bestVariant, List<T> variants, Map<String, Object> givens,
                                 String modelName, boolean variantsRankedAndTrackRunnersUp) {
        if(modelName == null || modelName.isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: modelName is empty or null");
            return ;
        }

        if(trackURL == null || trackURL.isEmpty()) {
            IMPLog.e(Tag, "Improve.track error: trackURL is empty or null");
            return ;
        }

        Map<String, Object> body = new HashMap<>();
        body.put(TYPE_KEY, DECISION_TYPE);
        body.put(MODEL_KEY, modelName);

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
    }


    public void trackEvent(String eventName) {
        trackEvent(eventName, null);
    }

    public void trackEvent(String eventName, Map<String, Object> properties) {
        if(trackURL == null || trackURL.isEmpty()) {
            IMPLog.w(Tag, "trackURL is empty or nil, event won't be tracked.");
            return ;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("type", "event");
        if (eventName != null) {
            body.put(EVENT_KEY, eventName);
        }
        if (properties != null) {
            body.put(PROPERTIES_KEY, properties);
        }

        postTrackingRequest(body);
    }

    public static void setHistoryId(String historyId) {
        sHistoryId = historyId;
    }

    protected String getHistoryId() {
        try {
            Class clz = Class.forName("ai.improve.android.HistoryIdProviderImp");
            Object o = clz.newInstance();

            Method method = clz.getDeclaredMethod("getHistoryId");
            String historyId = (String)method.invoke(o);
            return historyId;
        } catch (InstantiationException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (ClassNotFoundException e) {
        }
        return "";
    }

    protected void setBestVariant(Object variant, Map<String, Object> body) {
        body.put(DECISION_BEST_KEY, variant);
    }

    protected <T> void setCount(List<T> variants, Map<String, Object> body) {
        if(variants == null || variants.size() <= 0) {
            body.put(COUNT_KEY, 1);
        } else {
            body.put(COUNT_KEY, variants.size());
        }
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

    private void postTrackingRequest(Map<String, Object> body) {
        if (sHistoryId == null || sHistoryId.isEmpty()) {
            IMPLog.e(Tag, "historyId cannot be null");
            return;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_HEADER, APPLICATION_JSON);

        body = new HashMap<>(body);
        body.put(TIMESTAMP_KEY, ISO_TIMESTAMP_FORMAT.format(new Date()));
        body.put(HISTORY_ID_KEY, sHistoryId);
        body.put(MESSAGE_ID_KEY, UUID.randomUUID().toString());

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
}
