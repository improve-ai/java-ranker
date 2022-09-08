package ai.improve;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final String modelName;

    private String trackURL;

    private String trackApiKey;

    private DecisionTracker tracker;

    private ImprovePredictor predictor;

    private FeatureEncoder featureEncoder;

    private static final AtomicInteger seq = new AtomicInteger(0);

    protected boolean enableTieBreaker = true;

    private GivensProvider givensProvider;

    // Currently only set on Android; null on other platform
    private static GivensProvider defaultGivensProvider;

    private final static ModelMap instances = new ModelMap();

    private final Map<Integer, LoadListener> listeners = new ConcurrentHashMap<>();

    /**
     * It's an equivalent of DecisionModel(modelName, defaultTrackURL, defaultTrackApiKey)
     * We suggest to have the defaultTrackURL/defaultTrackApiKey set on startup before creating
     * any DecisionModel instances.
     * @param modelName Length of modelName must be in range [1, 64]; Only alphanumeric
     *                  characters([a-zA-Z0-9]), '-', '.' and '_' are allowed in the modelName
     *                  and the first character must be an alphanumeric one.
     * */
    public DecisionModel(String modelName) {
        this(modelName, sDefaultTrackURL, sDefaultTrackApiKey);
    }

    /**
     * @param modelName Length of modelName must be in range [1, 64]; Only alphanumeric
     *                  characters([a-zA-Z0-9]), '-', '.' and '_' are allowed in the modelName
     *                  and the first character must be an alphanumeric one.
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

    public static DecisionModel get(String modelName) {
        return instances.get(modelName);
    }

    public static void put(String modelName, DecisionModel decisionModel) {
        instances.put(modelName, decisionModel);
    }

    /**
     * Load a model synchronously. Calling this method would block the current thread, so please
     * try not do it in the UI thread.
     * @param modelUrl A url that can be a local file path, a remote http url that points to a
     *                 model file, or a bundled asset. Urls that ends with '.gz' are considered gzip
     *                 compressed, and will be handled appropriately. Bundled model asset urls
     *                 appears a bit different. Suppose that you have a bundled model file in folder
     *                 "assets/models/my_model.xgb.gz", then modelUrl should be
     *                 new URL("file:///android_asset/models/my_model.xgb").
     * @return Returns self.
     * @throws IOException Thrown if the model failed to load.
     */
    public DecisionModel load(URL modelUrl) throws IOException {
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

        loadAsync(modelUrl, listener);
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
     * @param modelUrl A url that can be a local file path, a remote http url that points to a
     *                 model file, or a bundled asset. Urls that ends with '.gz' are considered gzip
     *                 compressed, and will be handled appropriately. Bundled model asset urls
     *                 appears a bit different. Suppose that you have a bundled model file in folder
     *                 "assets/models/my_model.xgb.gz", then modelUrl should be
     *                 new URL("file:///android_asset/models/my_model.xgb").
     */
    public void loadAsync(URL modelUrl) {
        loadAsync(modelUrl, null);
    }

    /**
     * Notice that it's not recommended to call this method directly in an Android Activity as it
     * may cause leaks. Before we add a cancel method to allow aborting downloading tasks, you may
     * have to call loadAsync() in something like a Singleton class.
     * @deprecated  The callback method signature will likely have to change for multiple URLs
     * @param modelUrl A url that can be a local file path, a remote http url that points to a
     *                 model file, or a bundled asset. Urls that ends with '.gz' are considered gzip
     *                 compressed, and will be handled appropriately. Bundled model asset urls
     *                 appears a bit different. Suppose that you have a bundled model file in folder
     *                 "assets/models/my_model.xgb.gz", then modelUrl should be
     *                 new URL("file:///android_asset/models/my_model.xgb").
     * @param listener The callback that will run when the model is loaded.
     * */
    @Deprecated
    public void loadAsync(URL modelUrl, LoadListener listener) {
        int seq = getSeq();
        if(listener != null) {
            listeners.put(seq, listener);
        }
        ModelDownloader.download(modelUrl, new ModelDownloader.ModelDownloadListener() {
            @Override
            public void onFinish(ImprovePredictor predictor, IOException e) {
                LoadListener l = listeners.remove(seq);
                if(l == null) {
                    // Don't return here, just give a warning here.
                    IMPLog.d(Tag, "loadAsync finish loading model, but listener is null, " + modelUrl.toString());
                }

                if(e != null) {
                    if(l != null) {
                        l.onError(e);
                    }
                } else {
                    DecisionModel.this.setModel(predictor);

                    IMPLog.d(Tag, "loadAsync, finish loading model, " + modelUrl.toString());
                    if (l != null) {
                        l.onLoad(DecisionModel.this);
                    }
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

    // TODO: defaultGivensProvider is returned anyway even if setGivensProvider(null) is called.
    public GivensProvider getGivensProvider() {
        return givensProvider != null ? givensProvider : defaultGivensProvider;
    }

    public void setGivensProvider(GivensProvider givensProvider) {
        this.givensProvider = givensProvider;
    }

    // Currently only called on Android platform
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

    public synchronized ImprovePredictor getModel() {
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
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity,
     *                including nested dictionaries, lists, maps, strings, numbers, nulls, and
     *                booleans.
     * @return an IMPDecision object.
     * @throws IllegalArgumentException Thrown if variants is null or empty.
     * */
    public <T> Decision<T> chooseFrom(List<T> variants) {
        return given(null).chooseFrom(variants);
    }

    /**
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity,
     *                 including nested dictionaries, lists, maps, strings, numbers, nulls, and
     *                 booleans.
     * @param scores Scores of the variants.
     * @return A Decision object which has the variant with highest score as the best variant.
     * @throws IllegalArgumentException Thrown if variants or scores is null or empty; Thrown if
     * variants.size() != scores.size().
     */
    public <T> Decision<T> chooseFrom(List<T> variants, List<Double> scores) {
        return given(null).chooseFrom(variants, scores);
    }

    /**
     * This method is an alternative of chooseFrom(). An example here might be more expressive:
     * optimize({"style":["bold", "italic"], "size":[3, 5]})
     *       is equivalent to
     * chooseFrom([
     *      {"style":"bold", "size":3},
     *      {"style":"italic", "size":3},
     *      {"style":"bold", "size":5},
     *      {"style":"italic", "size":5},
     * ])
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity
     *                 like chooseFrom(). The value of the dictionary is expected to be a list.
     *                 If not, it would be automatically wrapped as a list containing a single item.
     *                 So chooseMultivariate({"style":["bold", "italic"], "size":3}) is equivalent to
     *                 chooseMultivariate({"style":["bold", "italic"], "size":[3]})
     * @return An IMPDecision object.
     */
    public Decision<Map<String, ?>> chooseMultivariate(Map<String, ?> variants) {
        return given(null).chooseMultivariate(variants);
    }

    /**
     * A shorthand of chooseMultivariate(variantMap).get().
     */
    public Map<String, ?> optimize(Map<String, ?> variants) {
        return given(null).optimize(variants);
    }

    /**
     * This is a short hand version of chooseFrom(variants).get() that returns the chosen result
     * directly.
     * @param variants See chooseFrom().
     * @return Returns the chosen variant
     * @throws IllegalArgumentException Thrown if variants number is 0.
     */
    @SafeVarargs
    public final <T> T which(T... variants) {
        return given(null).which(variants);
    }

    /**
     * This is a short hand version of chooseFrom(variants).get()
     * @param variants See chooseFrom().
     * @return Returns the chosen variant
     * @throws IllegalArgumentException Thrown if variants is null or empty.
     */
    public <T> T which(List<T> variants) {
        return given(null).which(variants);
    }

    /**
     * @param variants See chooseFrom()
     * @return A Decision object which has the first variant as the best.
     */
    public <T> Decision<T> chooseFirst(List<T> variants) {
        return given(null).chooseFirst(variants);
    }

    /**
     * A shorthand of chooseFirst().get().
     * @param variants See chooseFrom().
     * @return Returns the first variant.
     * @throws IllegalArgumentException Thrown if variants is null or empty.
     */
    public <T> T first(List<T> variants) {
        return given(null).first(variants);
    }

    /**
     * An alternative of first(list).
     * @param variants See chooseFrom().
     * @return Returns the first variant.
     * @throws IllegalArgumentException Thrown if variants number is 0.
     */
    @SafeVarargs
    public final <T> T first(T... variants) {
        return given(null).first(variants);
    }

    /**
     * Choose a random variant.
     * @param variants See chooseFrom()
     * @return A Decision object containing a random variant as the decision.
     * @throws IllegalArgumentException Thrown if variants is null or empty.
     */
    public <T> Decision<T> chooseRandom(List<T> variants) {
        return given(null).chooseRandom(variants);
    }

    /**
     * A shorthand of chooseRandom(variants).get()
     * @param variants See chooseFrom().
     * @return A random variant.
     * @throws IllegalArgumentException Thrown if variants is null or empty.
     */
    public <T> T random(List<T> variants) {
        return given(null).random(variants);
    }

    /**
     * An alternative of random(List).
     * @param variants See chooseFrom().
     * @return A random variant.
     * @throws IllegalArgumentException Thrown if variants number is 0.
     */
    @SafeVarargs
    public final <T> T random(T... variants) {
        return given(null).random(variants);
    }

    /**
     * @param givens Additional context info that will be used with each of the variants to calculate
     *              its feature vector.
     * @return A DecisionContext object.
     */
    public DecisionContext given(Map<String, ?> givens) {
        return new DecisionContext(this, givens);
    }

    protected Map<String, Object> combinedGivens(Map<String, Object> givens) {
        GivensProvider provider = getGivensProvider();
        return provider == null ? givens : provider.givensForModel(this, givens);
    }

    /**
     * If this method is called before the model is loaded, or errors occurred
     * while loading the model file, a randomly generated list of descending
     * Gaussian scores is returned.
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity,
     *                 including nested maps, arrays, strings, numbers, nulls, and booleans.
     * @throws IllegalArgumentException Thrown if variants is null or empty.
     * @return scores of the variants
     */
    public List<Double> score(List<?> variants) {
        return scoreInternal(variants, combinedGivens(null));
    }

    /**
     * If this method is called before the model is loaded, or errors occurred
     * while loading the model file, a randomly generated list of descending
     * Gaussian scores is returned.
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity,
     *                 including nested maps, arrays, strings, numbers, nulls, and booleans.
     * @param givens Additional context info that will be used with each of the variants to
     *               calculate the score, including the givens passed in through
     *               DecisionModel.given(givens) and the givens provided by the AppGivensProvider or
     *               other custom GivensProvider.
     * @throws IllegalArgumentException Thrown if variants is null or empty
     * @return scores of the variants
     */
    protected List<Double> scoreInternal(List<?> variants, Map<String, ?> givens) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants can't be null or empty");
        }

//        IMPLog.d(Tag, "givens: " + givens);

        if(predictor == null) {
            // When tracking a decision like this:
            // DecisionModel("model_name").chooseFrom(variants).get()
            // The model is not loaded. In this case, we return the scores quietly without logging an error.
            // IMPLog.e(Tag, "model is not loaded, a randomly generated list of Gaussian numbers is returned");
            return ModelUtils.generateDescendingGaussians(variants.size());
        }

        List<Double> result = new ArrayList<>();
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
     * This method should only be called on Android platform.
     * Adds the reward value to the most recent Decision for this model name for this installation.
     * The most recent Decision can be from a different DecisionModel instance or a previous session
     * as long as they have the same model name. If no previous Decision is found, the reward will
     * be ignored.
     * @param reward the reward to add. Must not be NaN, or Infinity.
     * @throws IllegalArgumentException Thrown if `reward` is NaN or Infinity
     * @throws IllegalStateException Thrown if trackURL is null, or called on non-Android platform.
     */
    public void addReward(double reward) {
        if(Double.isInfinite(reward) || Double.isNaN(reward)) {
            throw new IllegalArgumentException("reward must not be NaN or infinity");
        }

        if(DecisionTracker.persistenceProvider == null) {
            throw new IllegalStateException("DecisionModel.addReward() is only available on Android.");
        }

        if(tracker == null) {
            throw new IllegalStateException("trackURL can't be null when calling addReward()");
        }

        tracker.addRewardForModel(modelName, reward);
    }

    /**
     * Add reward for the provided decisionId
     * @param reward reward for the decision.
     * @param decisionId unique id of a decision.
     * @throws IllegalArgumentException Thrown if decisionId is null or empty; Thrown if reward
     * is NaN or Infinity.
     * @throws IllegalStateException Thrown if trackURL is null.
     * Adds the reward to a specific decision
     */
    public void addReward(double reward, String decisionId) {
        if(Double.isInfinite(reward) || Double.isNaN(reward)) {
            throw new IllegalArgumentException("reward must not be NaN or infinity");
        }

        if(Utils.isEmpty(decisionId)) {
            throw new IllegalArgumentException("decisionId can't be null or empty");
        }

        if(tracker == null) {
            throw new IllegalStateException("trackURL can't be null when calling addReward()");
        }

        tracker.addRewardForDecision(modelName, decisionId, reward);
    }

    /**
     * This method is likely to be changed in the future. Try not to use it in your code.
     * @param variants A list of variants to be ranked.
     * @param scores Scores of the variants.
     * @return a list of the variants ranked from best to worst by scores
     * @throws IllegalArgumentException Thrown if variants or scores is null; Thrown if
     * variants.size() not equal to scores.size().
     */
    public static <T> List<T> rank(List<T> variants, List<Double> scores) {
        if(variants == null || scores == null) {
            throw new IllegalArgumentException("variants or scores can't be null");
        }

        if(variants.size() != scores.size()) {
            throw new IllegalArgumentException("variants.size() must equal to scores.size()");
        }

        Integer[] indices = new Integer[variants.size()];
        for(int i = 0; i < variants.size(); ++i) {
            indices[i] = i;
        }

        Arrays.sort(indices, new Comparator<Integer>() {
            public int compare(Integer obj1, Integer obj2) {
                return scores.get(obj1) < scores.get(obj2) ? 1 : -1;
            }
        });

        List<T> result = new ArrayList<>(variants.size());
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

    protected static void clearInstances() {
        instances.clear();
    }

    protected static int sizeOfInstances() {
        return instances.size();
    }
}
