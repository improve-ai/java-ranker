package ai.improve.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.improve.android.hasher.FeatureEncoder;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;

public abstract class BaseIMPDecisionModel {
    private static final String Tag = "BaseIMPDecisionModel";

    private String modelName;

    private BaseIMPDecisionTracker tracker;

    private ImprovePredictor predictor;

    private FeatureEncoder featureEncoder;

    private static Random randomGenerator = new Random();

    private XXHashProvider xxHashProvider;

    public BaseIMPDecisionModel(String modelName, XXHashProvider xxHashProvider) {
        this.modelName = modelName;
        this.xxHashProvider = xxHashProvider;
    }

    public void setModel(ImprovePredictor predictor) {
        if(predictor == null) {
            IMPLog.e(Tag, "predictor is null");
            return ;
        }

        this.predictor = predictor;

        if((modelName != null && !modelName.isEmpty()) && !modelName.equals(predictor.getModelMetadata().getModelName())) {
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

    /**
     * @return an IMPDecision object
     * */
    public IMPDecision chooseFrom(List<Object> variants) {
        return new IMPDecision(this).chooseFrom(variants);
    }

    /**
     * @return an IMPDecision object
     * */
    public IMPDecision given(Map<String, Object> givens) {
        return new IMPDecision(this).given(givens);
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
    public List<Double> score(List<Object> variants, Map<String, ?> givens) {
        List<Double> result = new ArrayList<>();

        if(variants == null || variants.size() <= 0) {
            return result;
        }

        if(predictor == null) {
            IMPLog.e(Tag, "model is not loaded, a randomly generated list of Gaussian numbers is returned");
            return generateDescendingGaussians(variants.size());
        }

        List<FVec> encodedFeatures = featureEncoder.encodeVariants(variants, givens);
        for (FVec fvec : encodedFeatures) {
            result.add((double)predictor.predictSingle(fvec));
        }

        return result;
    }

    /**
     * If variants.size() != scores.size(), an IndexOutOfBoundException exception will be thrown
     * @return the variant with the best score
     * */
    public static Object topScoringVariant(List<Object> variants, List<Double> scores) {
        // check the size of variants and scores, and use the bigger one so that
        // an IndexOutOfBoundOfException would be thrown later
        int size = variants.size();
        if(scores.size() > variants.size()) {
            size = scores.size();
        }

        Object topVariant = variants.get(0);
        double bestScore = scores.get(0).doubleValue();
        for(int i = 1; i < size; ++i) {
            // scores.get(i) and variants.get(i) must be called for each loop.
            // We are relying on this to trigger an IndexOutOfBoundExeception
            // when variants.size() != scores.size()
            double score = scores.get(i).doubleValue();
            Object variant = variants.get(i);
            if(score > bestScore) {
                bestScore = score;
                topVariant = variant;
            }
        }
        return topVariant;
    }

    /**
     * If variants.size() != scores.size(), an IndexOutOfBoundException exception will be thrown
     * @return a list of the variants ranked from best to worst by scores
     * */
    public static List<Object> rank(List<Object> variants, List<Double> scores) {
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

    // Generate n = variants.count random (double) gaussian numbers
    // Sort the numbers descending and return the sorted list
    // The median value of the list is expected to have a score near zero
    private static List<Double> generateDescendingGaussians(int count) {
        Double[] scores = new Double[count];
        for (int i = 0; i < count; ++i) {
            scores[i] = randomGenerator.nextGaussian();
        }
        Arrays.sort(scores, Collections.reverseOrder());
        return Arrays.asList(scores);
    }
}
