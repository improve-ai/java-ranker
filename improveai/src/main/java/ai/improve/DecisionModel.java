package ai.improve;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ai.improve.downloader.ModelDownloader;
import ai.improve.encoder.FeatureEncoder;
import ai.improve.log.IMPLog;
import ai.improve.provider.GivensProvider;
import ai.improve.util.ModelMap;
import ai.improve.util.ModelUtils;
import ai.improve.util.Utils;
import ai.improve.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;

public class DecisionModel {
    public static final String Tag = "DecisionModel";

    private static String sDefaultTrackURL = null;

    private static String sDefaultTrackApiKey = null;

    private final Object lock = new Object();

    private String modelName;

    private String trackURL;

    private String trackApiKey;

    private DecisionTracker tracker;

    private ImprovePredictor predictor;

    private FeatureEncoder featureEncoder;

    private static AtomicInteger seq = new AtomicInteger(0);

    protected boolean enableTieBreaker = true;

    private GivensProvider givensProvider;

    private static GivensProvider defaultGivensProvider;

    public final static ModelMap instances = new ModelMap();

    /**
     * It's an equivalent of DecisionModel(modelName, defaultTrackURL, defaultTrackApiKey)
     * We suggest to have the defaultTrackURL/defaultTrackApiKey set on startup before creating
     * any DecisionModel instances.
     * */
    public DecisionModel(String modelName) {
        this(modelName, sDefaultTrackURL, sDefaultTrackApiKey);
    }

    /**
     * @param modelName Length of modelName must be in range [1, 64]; Only alphanumeric
     *                  characters([a-zA-Z0-9]), '-', '.' and '_' are allowed in the modelName
     *                  and the first character must be an alphanumeric one;
     * @param trackURL url for tracking decisions. If trackURL is null, no decisions would be
     *                 tracked. If trackURL is not a valid URL, an exception would be thrown.
     * @param trackApiKey will be attached to the header fields of all the post request for tracking
     * @throws IllegalArgumentException Thrown if an invalid modelName or an invalid trackURL
     */
    public DecisionModel(String modelName, String trackURL, String trackApiKey) {
        if(!isValidModelName(modelName)) {
            throw new IllegalArgumentException("invalid modelName: [" + modelName + "]");
        }
        this.modelName = modelName;
        this.trackApiKey = trackApiKey;

        setTrackURL(trackURL);

        this.givensProvider = defaultGivensProvider;
    }

    /**
     * WeakReference is used here to avoid Android activity leaks.
     * A sample activity "LeakTestActivity" is included in the sample project.
     * If WeakReference is removed, leaks can be observed when jumping between
     * "MainActivity" and "LeakTestActivity" many times while network speed is slow.
     */
    private Map<Integer, WeakReference<LoadListener>> listeners = new HashMap<>();

    public DecisionModel load(URL url) throws IOException {
        final IOException[] downloadException = {null};
        LoadListener listener = new LoadListener() {
            @Override
            public void onLoad(DecisionModel decisionModel) {
                synchronized (decisionModel.lock) {
                    decisionModel.lock.notifyAll();
                }
            }

            @Override
            public void onError(IOException e) {
                synchronized (DecisionModel.this.lock) {
                    downloadException[0] = e;
                    DecisionModel.this.lock.notifyAll();
                }
            }
        };

        loadAsync(url, listener);
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                IMPLog.e(Tag, e.getLocalizedMessage());
            }
        }

        if(downloadException[0] != null) {
            throw downloadException[0];
        }

        return this;
    }

    /**
     * @deprecated  The callback method signature will likely have to change for multiple URLs
     * */
    public void loadAsync(URL url, LoadListener listener) {
        int seq = getSeq();
        listeners.put(seq, new WeakReference<>(listener));
        ModelDownloader.download(url, new ModelDownloader.ModelDownloadListener() {
            @Override
            public void onFinish(ImprovePredictor predictor, IOException e) {
                LoadListener l = listeners.remove(seq).get();
                if(l != null) {
                    if(e != null) {
                        l.onError(e);
                        return ;
                    }
                    IMPLog.d(Tag, "loadAsync, finish loading model, " + url.toString());

                    DecisionModel.this.setModel(predictor);

                    l.onLoad(DecisionModel.this);
                } else {
                    // TODO Double check it later
                    // When calling DecisionModel.load(url), the anonymous listener might be
                    // released before the onFinish callback???
                    // I have seen the log 'onFinish, but listener is null' before, but can't
                    // reproduce it now.
                    IMPLog.d(Tag, "onFinish, but listener is null");
                }
            }
        });
    }

    public String getTrackURL() {
        return trackURL;
    }

    /**
     * @param trackURL url for decision tracking. If set as null, no decisions would be tracked.
     * @throws IllegalArgumentException Thrown if trackURL is nonnull and not a valid URL.
     * */
    public void setTrackURL(String trackURL) {
        if(trackURL == null) {
            this.trackURL = null;
            this.tracker = null;
        } else {
            if(!Utils.isValidURL(trackURL)) {
                throw new IllegalArgumentException("invalid trackURL: [" + trackURL + "]");
            }
            this.trackURL = trackURL;
            this.tracker = new DecisionTracker(trackURL, this.trackApiKey);
        }
    }

    public static String getDefaultTrackURL() {
        return sDefaultTrackURL;
    }

    /**
     * @param trackURL default trackURL for tracking decisions.
     * @throws IllegalArgumentException if trackURL is nonnull and not a valid url
     * */
    public static void setDefaultTrackURL(String trackURL) {
        if(trackURL != null && !Utils.isValidURL(trackURL)) {
            throw new IllegalArgumentException("invalid trackURL: " + trackURL);
        }
        sDefaultTrackURL = trackURL;
    }

    public String getTrackApiKey() {
        return trackApiKey;
    }

    public void setTrackApiKey(String trackApiKey) {
        this.trackApiKey = trackApiKey;
        if(tracker != null) {
            tracker.setTrackApiKey(trackApiKey);
        }
    }

    public String getDefaultTrackApiKey() {
        return sDefaultTrackApiKey;
    }

    public static void setDefaultTrackApiKey(String defaultTrackApiKey) {
        sDefaultTrackApiKey = defaultTrackApiKey;
    }

    public GivensProvider getGivensProvider() {
        return givensProvider;
    }

    public void setGivensProvider(GivensProvider givensProvider) {
        this.givensProvider = givensProvider;
    }

    protected static GivensProvider getDefaultGivensProvider() {
        return defaultGivensProvider;
    }

    public static void setDefaultGivensProvider(GivensProvider givensProvider) {
        defaultGivensProvider = givensProvider;
    }

    public synchronized void setModel(ImprovePredictor predictor) {
        if(predictor == null) {
            IMPLog.e(Tag, "predictor is null");
            return ;
        }

        this.predictor = predictor;

        if(!modelName.equals(predictor.getModelMetadata().getModelName())){
            IMPLog.w(Tag, "Model names don't match: current model name [" + modelName
                    + "], model name extracted [" + predictor.getModelMetadata().getModelName() +"], ["
                    + modelName + "] will be used.");
        }

        featureEncoder = new FeatureEncoder(predictor.getModelMetadata().getModelSeed(),
                predictor.getModelMetadata().getModelFeatureNames());
    }

    public ImprovePredictor getModel() {
        return predictor;
    }

    public String getModelName() {
        return modelName;
    }

    protected DecisionTracker getTracker() {
        return tracker;
    }

    protected FeatureEncoder getFeatureEncoder() {
        return featureEncoder;
    }

    /**
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity, including nested dictionaries,
     * lists, maps, strings, numbers, nulls, and booleans.
     * @return an IMPDecision object.
     * */
    public <T> Decision chooseFrom(List<T> variants) {
        return new Decision(this).chooseFrom(variants);
    }

    /**
     * This method is an alternative of chooseFrom(). An example here might be more expressive:
     * chooseMutilVariate({"style":["bold", "italic"], "size":[3, 5]})
     *       is equivalent to
     * chooseFrom([
     *      {"style":"bold", "size":3},
     *      {"style":"italic", "size":3},
     *      {"style":"bold", "size":5},
     *      {"style":"italic", "size":5},
     * ])
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity like chooseFrom().
     * The value of the dictionary is expected to be an NSArray. If not, it would be treated as an one-element NSArray anyway.
     * So chooseMutilVariate({"style":["bold", "italic", "size":3}) is equivalent to chooseMutilVariate({"style":["bold", "italic", "size":[3]})
     * @return An IMPDecision object.
     * */
    public Decision chooseMultiVariate(Map<String, ?> variants) {
        if(variants == null || variants.size() <= 0) {
            // Let it pass here.
            // Exception would be thrown later when calling get()
            return chooseFrom(null);
        }

        List allKeys = new ArrayList();

        List<List> categories = new ArrayList();
        for(Map.Entry<String, ?> entry : variants.entrySet()) {
            if(entry.getValue() instanceof List) {
                categories.add((List)entry.getValue());
            } else {
                categories.add(Arrays.asList(entry.getValue()));
            }
            allKeys.add(entry.getKey());
        }

        List<Map> combinations = new ArrayList();
        for(int i = 0; i < categories.size(); ++i) {
            List category = categories.get(i);
            List<Map> newCombinations = new ArrayList();
            for(int m = 0; m < category.size(); ++m) {
                if(combinations.size() == 0) {
                    Map newVariant = new HashMap();
                    newVariant.put(allKeys.get(i), category.get(m));
                    newCombinations.add(newVariant);
                } else {
                    for(int n = 0; n < combinations.size(); ++n) {
                        Map newVariant = new HashMap(combinations.get(n));
                        newVariant.put(allKeys.get(i), category.get(m));
                        newCombinations.add(newVariant);
                    }
                }
            }
            combinations = newCombinations;
        }

        return chooseFrom(combinations);
    }

    /**
     * This is a short hand version of chooseFrom(variants).get() that returns the chosen result
     * directly.
     * @param variants See chooseFrom().
     *                 When the only argument is an NSArray, it's equivalent to calling
     *                 chooseFrom(firstVariant).get();
     *                 When the only argument is an NSDictionary, it's equivalent to calling
     *                 chooseMultiVariate(firstVariant).get();
     *                 When there are two or more arguments, all the arguments would form a
     *                 list and be passed to chooseFrom();
     * @return Returns the chosen variant
     * @throws IllegalArgumentException Thrown if variants is null or empty; or if there's only one
     * variant and it's not a List or Map.
     * */
    public Object which(Object... variants) {
        if(variants == null || variants.length <= 0) {
            throw new IllegalArgumentException("should at least provide one variant.");
        } else if(variants.length == 1) {
            if(variants[0] instanceof List) {
                return chooseFrom((List)variants[0]).get();
            } else if(variants[0] instanceof Map) {
                return chooseMultiVariate((Map)variants[0]).get();
            }
            throw new IllegalArgumentException("If only one argument, it must be a List or Map");
        } else {
            return chooseFrom(Arrays.asList(variants)).get();
        }
    }

    /**
     * @return an IMPDecision object
     * */
    public Decision given(Map<String, Object> givens) {
        Decision decision = new Decision(this);
        decision.setGivens(givens);
        return decision;
    }

    protected Map<String, Object> combinedGivens(Map<String, Object> givens) {
        return givensProvider == null ? givens : givensProvider.givensForModel(this, givens);
    }

    public <T> List<Double> score(List<T> variants) {
        return this.score(variants, null);
    }

    /**
     * Returns a list of double scores. If variants is null or empty, an empty
     * list is returned.
     *
     * If this method is called before the model is loaded, or errors occurred
     * while loading the model file, a randomly generated list of descending
     * Gaussian scores is returned.
     *
     * @return a list of double scores.
     *
     * */
    public <T> List<Double> score(List<T> variants, Map<String, ?> givens) {
        List<Double> result = new ArrayList<>();

        if(variants == null || variants.size() <= 0) {
            return result;
        }

        if(predictor == null) {
            // When tracking a decision like this:
            // DecisionModel("model_name").chooseFrom(variants).get()
            // The model is not loaded. In this case, we return the scores quietly without logging an error.
            // IMPLog.e(Tag, "model is not loaded, a randomly generated list of Gaussian numbers is returned");
            return ModelUtils.generateDescendingGaussians(variants.size());
        }

        List<FVec> encodedFeatures = featureEncoder.encodeVariants(variants, givens);
        for (FVec fvec : encodedFeatures) {
            if(enableTieBreaker) {
                // add a very small random number to randomly break ties
                double smallNoise = Math.random() * Math.pow(2, -23);
                result.add((double) predictor.predictSingle(fvec) + smallNoise);
            } else {
                result.add((double) predictor.predictSingle(fvec));
            }
        }

        return result;
    }

    /**
     * Adds the reward value to the most recent Decision for this model name for this installation.
     * The most recent Decision can be from a different DecisionModel instance or a previous session
     * as long as they have the same model name. If no previous Decision is found, the reward will
     * be ignored.
     * This method should only be called on Android platform; Otherwise, a RuntimeException would
     * be thrown.
     * @param reward the reward to add. Must not be NaN, positive infinity, or negative infinity
     * @throws IllegalArgumentException Thrown if `reward` is NaN or +-Infinity
     * @throws IllegalStateException Thrown if trackURL is null
     * */
    public void addReward(double reward) {
        if(Double.isInfinite(reward) || Double.isNaN(reward)) {
            throw new IllegalArgumentException("reward must not be NaN or infinity");
        }

        if(DecisionTracker.persistenceProvider == null) {
            // TODO
            // I can't think of an appropriate exception to throw here? Ideas?
            // UnsupportedOperationException?
            throw new RuntimeException("DecisionModel.addReward() is only available for Android.");
        }

        if(tracker == null) {
            throw new IllegalStateException("trackURL can't be null when calling addReward()");
        }

        tracker.addRewardForModel(modelName, reward);
    }

    /**
     * Adds the reward to a specific decision
     * */
    protected void addRewardForDecision(String decisionId, double reward) {
        if(Double.isInfinite(reward) || Double.isNaN(reward)) {
            throw new IllegalArgumentException("reward must not be NaN or infinity");
        }

        if(DecisionTracker.persistenceProvider == null) {
            throw new RuntimeException("DecisionModel.addReward() is only available for Android.");
        }

        if(tracker == null) {
            throw new IllegalStateException("trackURL can't be null when calling addReward()");
        }

        tracker.addRewardForDecision(modelName, decisionId, reward);
    }

    /**
     * This method is likely to be changed in the future. Try not to use it in your code.
     *
     * If variants.size() != scores.size(), an IndexOutOfBoundException exception will be thrown
     * @return a list of the variants ranked from best to worst by scores
     * */
    public static <T> List<Object> rank(List<T> variants, List<Double> scores) {
        // check the size of variants and scores, and use the bigger one so that
        // an IndexOutOfBoundOfException would be thrown later
        int size = variants.size();
        if(scores.size() > variants.size()) {
            size = scores.size();
        }

        Integer[] indices = new Integer[variants.size()];
        for(int i = 0; i < size; ++i) {
            indices[i] = i;
        }

        Arrays.sort(indices, new Comparator<Integer>() {
            public int compare(Integer obj1, Integer obj2) {
                return scores.get(obj1) < scores.get(obj2) ? 1 : -1;
            }
        });

        List<Object> result = new ArrayList<>(variants.size());
        for(int i = 0; i < indices.length; ++i) {
            result.add(variants.get(indices[i]));
        }

        return result;
    }

    public interface LoadListener {
        void onLoad(DecisionModel decisionModel);

        void onError(IOException e);
    }

    private int getSeq() {
        return seq.getAndIncrement();
    }

    private boolean isValidModelName(String modelName) {
        return modelName != null && modelName.matches("^[a-zA-Z0-9][\\w\\-.]{0,63}$");
    }
}
