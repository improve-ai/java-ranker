package ai.improve.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.improve.android.hasher.FeatureEncoder;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;

public abstract class BaseIMPDecisionModel {
    public static final String Tag = "IMPDecisionModel";

    private String modelName;

    private BaseIMPDecisionTracker tracker;

    private ImprovePredictor predictor;

    private FeatureEncoder featureEncoder;

    private Random randomGenerator = new Random();

    private XXHashProvider xxHashProvider;

//    public static BaseIMPDecisionModel loadFromAsset(Context context, String filename) {
//        BaseIMPDecisionModel model = new BaseIMPDecisionModel("");
//        try {
//            ImprovePredictor predictor = ModelDownloader.fromAsset(context, filename);
//            model.setModel(predictor);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//        return model;
//    }

    public BaseIMPDecisionModel(String modelName, XXHashProvider xxHashProvider) {
        this.modelName = modelName;
        this.xxHashProvider = xxHashProvider;
    }

    public void setModel(ImprovePredictor predictor) {
        this.predictor = predictor;

        if((modelName == null || modelName.isEmpty()) && !modelName.equals(predictor.getModelMetadata().getModelName())) {
            IMPLog.w(Tag, "Model names don't match: Current model name [" + modelName
                    + "], new model Name [" + predictor.getModelMetadata().getModelName() +"]");
        }
        this.modelName = predictor.getModelMetadata().getModelName();

        featureEncoder = new FeatureEncoder(predictor.getModelMetadata().getModelSeed(),
                predictor.getModelMetadata().getModelFeatureNames(), this.xxHashProvider);
    }

    public String getModelName() {
        return modelName;
    }

    public BaseIMPDecisionTracker getTracker() {
        return tracker;
    }

    public BaseIMPDecisionModel track(BaseIMPDecisionTracker tracker) {
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

    public static Object topScoringVariant(List<Object> variants, List<Double> scores) {
        if(variants.size() != scores.size() || variants.size() <= 0) {
            return null;
        }

        Object topVariant = variants.get(0);
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

    public static List<Object> rank(List<Object> variants, List<Double> scores) {
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

        List<Object> result = new ArrayList<>(variants.size());
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
