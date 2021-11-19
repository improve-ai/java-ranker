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
import ai.improve.util.ModelUtils;
import ai.improve.util.Utils;
import ai.improve.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;

public class DecisionModel {
    public static final String Tag = "DecisionModel";

    public static String defaultTrackURL = null;

    private final Object lock = new Object();

    private String modelName;

    private String trackURL;

    private DecisionTracker tracker;

    private ImprovePredictor predictor;

    private FeatureEncoder featureEncoder;

    private static AtomicInteger seq = new AtomicInteger(0);

    protected boolean enableTieBreaker = true;

    private GivensProvider givensProvider;

    private static GivensProvider defaultGivensProvider;

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

    /**
     * It's an equivalent of DecisionModel(modelName, DecisionModel.defaultTrackURL)
     * We suggest to have the defaultTrackURL set on startup before creating any DecisionModel
     * instances.
     * */
    public DecisionModel(String modelName) {
        this(modelName, defaultTrackURL);
    }

    /**
     * @param modelName Length of modelName must be in range [1, 64]; Only alphanumeric
     *                  characters([a-zA-Z0-9]), '-', '.' and '_' are allowed in the modelName
     *                  and the first character must be an alphanumeric one; Despite the rules above,
     *                  null is also a valid model name.
     * @param trackURL url for tracking decisions. If trackURL is nil, no decisions would be
     *                 tracked.
     * @exception IllegalArgumentException in case of an invalid modelName or an invalid trackURL
     */
    public DecisionModel(String modelName, String trackURL) {
        if(!isValidModelName(modelName)) {
            throw new IllegalArgumentException("invalid modelName: [" + modelName + "]");
        }
        this.modelName = modelName;

        setTrackURL(trackURL);

        this.givensProvider = defaultGivensProvider;
    }

    public String getTrackURL() {
        return this.trackURL;
    }

    /**
     * @param trackURL url for decision tracking. If set as null, no decisions would be tracked.
     * @throws IllegalArgumentException in case of in non-null invalid trackURL
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
            this.tracker = new DecisionTracker(trackURL);
        }
    }

    public void setDefaultTrackURL(String trackURL) {
        defaultTrackURL = trackURL;
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

        if(modelName == null) {
            modelName = predictor.getModelMetadata().getModelName();
        } else if(!modelName.equals(predictor.getModelMetadata().getModelName())){
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
     * @return an IMPDecision object
     * */
    public <T> Decision chooseFrom(List<T> variants) {
        return new Decision(this).chooseFrom(variants);
    }

    /**
     * @return an IMPDecision object
     * */
    public Decision given(Map<String, Object> givens) {
        return new Decision(this).given(givens);
    }

    protected Map<String, Object> combinedGivens(Map<String, Object> givens) {
        GivensProvider gp = getGivensProvider();
        if(gp != null) {
            return gp.givensForModel(this, givens);
        } else {
            return givens;
        }
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
     * @exception IllegalArgumentException in case of NaN or +-Infinity
     * */
    public void addReward(double reward) {
        if(Double.isInfinite(reward) || Double.isNaN(reward)) {
            throw new IllegalArgumentException("reward must not be NaN or infinity");
        }

        if(DecisionTracker.persistenceProvider == null) {
            throw new RuntimeException("DecisionModel.addReward() is only available for Android.");
        }

        if(tracker == null) {
            String msg = String.format("trackURL of model(%s) not set, this reward won't be tracked", modelName);
            IMPLog.w(Tag, msg);
            return ;
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
            String msg = String.format("trackURL of model(%s) not set, this reward won't be tracked", modelName);
            IMPLog.w(Tag, msg);
            return ;
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
        if(modelName == null) {
            return true;
        }
        return modelName.matches("^[a-zA-Z0-9][\\w\\-.]{0,63}$");
    }
}
