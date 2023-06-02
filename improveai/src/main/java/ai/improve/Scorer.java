package ai.improve;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import ai.improve.downloader.ModelDownloader;
import ai.improve.encoder.FeatureEncoder;
import ai.improve.log.IMPLog;
import ai.improve.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;

/**
 Scores items with optional context using a CoreML model.
 */
public class Scorer {
    public static final String Tag = "Scorer";

    private final CountDownLatch loadModelSignal = new CountDownLatch(1);

    private ImprovePredictor predictor;

    private FeatureEncoder featureEncoder;

    /**
     * Initialize a Scorer instance.
     * @param modelUrl URL of a plain or gzip compressed CoreML model resource.
     *                 https://improve.ai/model.xgb
     *                 https://improve.ai/model.xgb.gz
     *                 file:///android_asset/models/model.xgb(Bundled models in assets folder)
     * @throws IOException, InterruptedException -> An error if the model cannot be loaded or if the metadata cannot be extracted.
     */
    public Scorer(URL modelUrl) throws IOException, InterruptedException {
        loadModel(modelUrl);
        if(predictor == null) {
            throw new IOException("Failed to load model " + modelUrl);
        }
    }

    /**
     * Uses the model to score a list of items.
     * @param items the list of items to score.
     * @throws Exception -> error if the items list is empty or if there's an issue with the prediction.
     * @return List<Double> an array of `Double` values representing the scores of the items.
     */
    public List<Double> score(List<?> items) {
        return score(items, null);
    }

    /**
     * Uses the model to score a list of items with the given context.
     * @param items the list of items to score.
     * @param context extra context info that will be used with each of the item to get its score.
     * @throws Exception -> error if the items list is empty or if there's an issue with the prediction.
     * @return List<Double> an array of `Double` values representing the scores of the items.
     */
    public List<Double> score(List<?> items, Object context) {
        return score(items, context, Math.random());
    }

    /**
     * Uses the model to score a list of items with the given context.
     * @param items the list of items to score.
     * @param context extra context info that will be used with each of the item to get its score.
     * @param noise value in [0, 1) which will be used to slightly modify the feature values
     * @throws Exception -> error if the items list is empty or if there's an issue with the prediction.
     * @return List<Double> an array of `Double` values representing the scores of the items.
     */
    protected  List<Double> score(List<?> items, Object context, double noise) {
        if(items == null || items.size() <= 0) {
            throw new IllegalArgumentException("items can't be null or empty");
        }

        List<Double> result = new ArrayList<>();
        List<FVec> encodedFeatures = featureEncoder.encodeFeatureVectors(items, context, noise);
        for (FVec fvec : encodedFeatures) {
            // add a very small random number to randomly break ties
            double smallNoise = Math.random() * Math.pow(2, -23);
            result.add((double) predictor.predictSingle(fvec) + smallNoise);
        }
        return result;
    }

    private void setModel(ImprovePredictor predictor) {
        this.predictor = predictor;

        featureEncoder = new FeatureEncoder(predictor.getModelMetadata().getModelFeatureNames(),
                predictor.getModelMetadata().getStringTables(),
                predictor.getModelMetadata().getModelSeed());
    }

    private void loadModel(URL modelUrl) throws InterruptedException {
        ModelDownloader.download(modelUrl, (predictor, e) -> {
            if(e != null) {
                IMPLog.e(Tag, "Failed to load model, " + e.getMessage());
                loadModelSignal.countDown();
                return;
            }

            setModel(predictor);
            loadModelSignal.countDown();

        });
        loadModelSignal.await();
    }
}
