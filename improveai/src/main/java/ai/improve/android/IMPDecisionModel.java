package ai.improve.android;

import android.content.Context;
import android.text.TextUtils;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ai.improve.android.hasher.XXFeatureEncoder;
import ai.improve.android.spi.ModelDownloader;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;

public class IMPDecisionModel {
    public static final String Tag = "IMPDecisionModel";

    private String modelName;

    private IMPDecisionTracker tracker;

    private ImprovePredictor predictor;

    private XXFeatureEncoder featureEncoder;

    private RandomGenerator randomGenerator = new JDKRandomGenerator();

    public static IMPDecisionModel loadFromAsset(Context context, String filename) {
        IMPDecisionModel model = new IMPDecisionModel("");
        try {
            ImprovePredictor predictor = ModelDownloader.fromAsset(context, filename);
            model.setModel(predictor);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return model;
    }

    public IMPDecisionModel(String modelName) {
        this.modelName = modelName;
    }

    public void setModel(ImprovePredictor predictor) {
        this.predictor = predictor;

        if(!TextUtils.isEmpty(modelName) && !modelName.equals(predictor.getModelMetadata().getModelName())) {
            IMPLog.w(Tag, "Model names don't match: Current model name [" + modelName
                    + "], new model Name [" + predictor.getModelMetadata().getModelName() +"]");
        }
        this.modelName = predictor.getModelMetadata().getModelName();

        featureEncoder = new XXFeatureEncoder(predictor.getModelMetadata().getModelSeed(),
                predictor.getModelMetadata().getModelFeatureNames());
    }

    public IMPDecisionModel track(IMPDecisionTracker tracker) {
        this.tracker = tracker;
        return this;
    }

    public IMPDecision chooseFrom(List<Object> variants) {
        return new IMPDecision(this).chooseFrom(variants);
    }

    public IMPDecision given(Map<String, Object> givens) {
        return new IMPDecision(this).given(givens);
    }

    public List<Double> score(List<Object> variants) {
        return this.score(variants, null);
    }

    public List<Double> score(List<Object> variants, Map<String, ?> givens) {
        List<Double> result = new ArrayList<>();

        if(variants == null || variants.size() <= 0) {
            return result;
        }

        if(predictor == null) {
            return generateDescendingGaussians(variants.size());
        }

        List<FVec> encodedFeatures = featureEncoder.encodeVariants(variants, givens);
        for (FVec fvec : encodedFeatures) {
            result.add((double)predictor.predictSingle(fvec));
        }

        return result;
    }

    public static <T> T topScoringVariant(List<T> variants, List<Number> scores) {
        if(variants.size() != scores.size() || variants.size() <= 0) {
            return null;
        }

        T topVariant = variants.get(0);
        double bestScore = scores.get(0).doubleValue();
        for(int i = 1; i < variants.size(); ++i) {
            double score = scores.get(i).doubleValue();
            if(score > bestScore) {
                bestScore = score;
                topVariant = variants.get(i);
            }
        }

        return topVariant;
    }

    public static <T> List<T> rank(List<T> variants, List<Float> scores) {
        if(variants.size() != scores.size()) {
            return variants;
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

    private List<Double> generateDescendingGaussians(int count) {
        Double[] scores = new Double[count];
        for (int i = 0; i < count; ++i) {
            scores[i] = randomGenerator.nextGaussian();
        }
        Arrays.sort(scores);
        return Arrays.asList(scores);
    }
}
