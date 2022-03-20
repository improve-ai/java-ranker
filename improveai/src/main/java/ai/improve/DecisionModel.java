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

    // Currently only set on Android; null on other platform
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
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity,
     *                including nested dictionaries, lists, maps, strings, numbers, nulls, and
     *                booleans.
     * @return an IMPDecision object.
     * */
    public <T> Decision chooseFrom(List<T> variants) {
        return new DecisionContext(this, null).chooseFrom(variants);
    }

    /**
     * This method is an alternative of chooseFrom(). An example here might be more expressive:
     * chooseMultiVariate({"style":["bold", "italic"], "size":[3, 5]})
     *       is equivalent to
     * chooseFrom([
     *      {"style":"bold", "size":3},
     *      {"style":"italic", "size":3},
     *      {"style":"bold", "size":5},
     *      {"style":"italic", "size":5},
     * ])
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity
     *                 like chooseFrom(). The value of the dictionary is expected to be a List.
     *                 If not, it would be treated as an one-element List anyway. So
     *                 chooseMultiVariate({"style":["bold", "italic"], "size":3}) is equivalent to
     *                 chooseMultiVariate({"style":["bold", "italic"], "size":[3]})
     * @return An IMPDecision object.
     * */
    public Decision chooseMultiVariate(Map<String, ?> variants) {
        return new DecisionContext(this, null).chooseMultiVariate(variants);
    }

    /**
     * This is a short hand version of chooseFrom(variants).get() that returns the chosen result
     * directly.
     * @param variants See chooseFrom().
     *                 When the only argument is a List, it's equivalent to calling
     *                 chooseFrom(variants).get();
     *                 When the only argument is a Map, it's equivalent to calling
     *                 chooseMultiVariate(variants).get();
     *                 When there are two or more arguments, all the arguments would form a
     *                 list and be passed to chooseFrom();
     * @return Returns the chosen variant
     * @throws IllegalArgumentException Thrown if variants is null or empty; or if there's only one
     * variant and it's not a List or Map.
     * */
    public Object which(Object... variants) {
        return new DecisionContext(this, null).which(variants);
    }

    /**
     * @return an IMPDecision object
     * */
    public DecisionContext given(Map<String, Object> givens) {
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
    public <T> List<Double> score(List<T> variants) {
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
    protected  <T> List<Double> scoreInternal(List<T> variants, Map<String, ?> givens) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants can't be null or empty");
        }

        IMPLog.d(Tag, "givens: " + givens);

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
     * */
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
    public static <T> List<T> rank(List<T> variants, List<Double> scores) {
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

        List<T> result = new ArrayList<T>(variants.size());
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
