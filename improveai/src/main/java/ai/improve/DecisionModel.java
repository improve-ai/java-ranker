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

    /**
     * WeakReference is used here to avoid Android activity leaks.
     * A sample activity "LeakTestActivity" is included in the sample project.
     * If WeakReference is removed, leaks can be observed when jumping between
     * "MainActivity" and "LeakTestActivity" many times while network speed is slow.
     */
    private Map<Integer, WeakReference<LoadListener>> listeners = new HashMap<>();

    public static DecisionModel load(URL url) throws IOException {
        DecisionModel decisionModel = new DecisionModel("");
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
                synchronized (decisionModel.lock) {
                    downloadException[0] = e;
                    decisionModel.lock.notifyAll();
                }
            }
        };

        decisionModel.loadAsync(url, listener);
        synchronized (decisionModel.lock) {
            try {
                decisionModel.lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                IMPLog.e(Tag, e.getLocalizedMessage());
            }
        }

        if(downloadException[0] != null) {
            throw downloadException[0];
        }

        return decisionModel;
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

    public DecisionModel(String modelName) {
        this(modelName, defaultTrackURL);
    }

    /**
     * @param modelName Length of modelName must be in range [1, 64]; Only alhpanumeric characters([a-zA-Z0-9]), '-', '.' and '_'
     * are allowed in the modenName and the first character must be an alphnumeric one; nil is also a valid model name.
     * @param trackURL url for tracking decisions. If trackURL is nil, no decisions would be tracked.
     * @exception RuntimeException in case of an invalid modelName
     */
    public DecisionModel(String modelName, String trackURL) {
        if(!isValidModelName(modelName)) {
            throw new RuntimeException("invalid modelName: [" + modelName + "]");
        }
        this.modelName = modelName;


        this.trackURL = trackURL;
    }

    public String getTrackURL() {
        return this.trackURL;
    }

    public void setTrackURL(String trackURL) {
        this.trackURL = trackURL;
        this.tracker = new DecisionTracker(trackURL);
    }

    public void setDefaultTrackURL(String trackURL) {
        defaultTrackURL = trackURL;
    }

    public synchronized void setModel(ImprovePredictor predictor) {
        if(predictor == null) {
            IMPLog.e(Tag, "predictor is null");
            return ;
        }

        this.predictor = predictor;

        if((modelName != null && !modelName.isEmpty()) && !modelName.equals(predictor.getModelMetadata().getModelName())) {
            IMPLog.w(Tag, "Model names don't match: Current model name [" + modelName
                    + "], new model Name [" + predictor.getModelMetadata().getModelName() +"] will be used.");
        }
        this.modelName = predictor.getModelMetadata().getModelName();

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
        if(Utils.isEmpty(modelName)) {
            return false;
        }
        return modelName.matches("^[a-zA-Z0-9][\\w\\-.]{0,63}$");
    }
}
