package ai.improve.android.chooser;

import ai.improve.android.ScoredVariant;
import ai.improve.android.hasher.FeatureEncoder;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ImproveChooser {

    private static final RandomGenerator randomGenerator = new JDKRandomGenerator();
    private static final double NOISE_LEVEL = 10000000000000d;

    private ImprovePredictor predictor;
    private List<Number> table;
    private int modelSeed;


    public ImproveChooser(ImprovePredictor predictor, List<Number> table, int modelSeed) {
        this.predictor = predictor;
        this.table = table;
        this.modelSeed = modelSeed;
    }

    /**
     *
     * @param variants
     * @param context
     * @return
     */
    public List<ScoredVariant> score(List<Object> variants, Map<String, Object> context) {

        List<Object> shuffledVariants = new ArrayList<>(variants); // ensure mutability
        //Collections.shuffle(shuffledVariants);

        List<Map<Integer, Double>> features = encodeVariants(variants, context);
        List<Number> scores = batchPrediction(features);
        if(scores.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredVariant> scored = new ArrayList();

        for(int i = 0; i < scores.size(); ++i) {
            double score = scores.get(i).doubleValue() + 1;

            double flexiScore = score + (randomGenerator.nextDouble() / NOISE_LEVEL);
            ScoredVariant p = new ScoredVariant(shuffledVariants.get(i), flexiScore);
            scored.add(p);
        }

        Collections.sort(scored);

        return scored;
    }

    /**
     *
     * @param features
     * @return
     */
    private List<Number> batchPrediction(List<Map<Integer, Double>> features) {
        List<Number> result = new ArrayList<>(features.size());
        for (Map<Integer, Double> feat : features) {
            FVec fVec = FVec.Transformer.fromMap(feat);
            result.add(predictor.predictSingle(fVec));
        }
        return result;
    }

    /**
     *
     * @param variants
     * @param context
     * @return
     */
    private List<Map<Integer, Double>> encodeVariants(List variants, Map<String, Object> context) {
        if (context == null) {
            // Safe nil context handling
            context = Collections.emptyMap();
        }
        FeatureEncoder encoder = new FeatureEncoder(table, modelSeed);
        Map<Integer, Double> encodedContext = encoder.encodeFeatures("context", context);

        List<Map<Integer, Double>> result = new ArrayList<>(variants.size());
        for (Object variant : variants) {
            result.add(encoder.encodeFeatures("variant", variant, encodedContext));
        }

        return result;
    }
    

}
