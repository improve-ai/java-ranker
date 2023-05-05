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

public class Scorer {
    public static final String Tag = "Scorer";

    private CountDownLatch loadModelSignal = new CountDownLatch(1);

    private ImprovePredictor predictor;

    private FeatureEncoder featureEncoder;

    private boolean enableTieBreaker = true;

    public Scorer(String modelUrl) throws IOException, InterruptedException {
        loadModel(new URL(modelUrl));
        if(predictor == null) {
            throw new IOException("Failed to load model " + modelUrl);
        }
    }

    public <T> List<Double> score(List<?> items) {
        return score(items, null);
    }

    public <T> List<Double> score(List<?> items, T context) {
        if(items == null || items.size() <= 0) {
            throw new IllegalArgumentException("variants can't be null or empty");
        }

        List<Double> result = new ArrayList<>();
        List<FVec> encodedFeatures = featureEncoder.encodeFeatureVectors(items, context, Math.random());
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

    private synchronized void setModel(ImprovePredictor predictor) {
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
