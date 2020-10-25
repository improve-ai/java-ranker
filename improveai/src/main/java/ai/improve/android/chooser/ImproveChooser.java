package ai.improve.android.chooser;

import ai.improve.android.hasher.FeatureEncoder;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import android.util.Pair;
import biz.k11i.xgboost.util.FVec;

import java.util.*;

public class ImproveChooser {

    private ImprovePredictor predictor;
    private List<Number> table;
    private int modelSeed;


    public ImproveChooser(ImprovePredictor predictor, List<Number> table, int modelSeed) {
        this.predictor = predictor;
        this.table = table;
        this.modelSeed = modelSeed;
    }

    public Object choose(List variants, Map<String, Object> context) {
        List<Map<Integer, Double>> features = encodeVariants(variants, context);
        List<Number> scores = batchPrediction(features);
        return findBestSample(variants, scores);
    }

    public List<Object> score(List variants, Map<String, Object> context) {

        List shuffledVariants = new ArrayList(variants); // ensure mutability
        Collections.shuffle(variants);

        List<Map<Integer, Double>> features = encodeVariants(variants, context);
        List<Number> scores = batchPrediction(features);
        if(scores == null || scores.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<Pair> scored = new ArrayList();

        for(int i = 0; i < scores.size(); ++i) {
            Pair p = new Pair(scores.get(i), shuffledVariants.get(i));
            scored.add(p);
        }

        Collections.sort(scored, new Comparator<Pair>() {
            @Override
            public int compare(Pair lhs, Pair rhs) {
                return Double.compare(((Number)lhs.first).doubleValue(), ((Number)rhs.first).doubleValue());
            }
        });

        List<Object> sorted = new ArrayList<>(scored.size());

        for(Pair p: scored) {
            sorted.add(p.second);
        }
        return sorted;
    }

    private List<Number> batchPrediction(List<Map<Integer, Double>> features) {
        List<Number> result = new ArrayList<>(features.size());
        for (Map<Integer, Double> feat : features) {
            FVec fVec = FVec.Transformer.fromMap(feat);
            result.add(predictor.predictSingle(fVec));
        }
        return result;
    }

    private List<Map<Integer, Double>> encodeVariants(List variants, Map<String, Object> context) {
        if (context == null) {
            // Safe nil context handling
            context = Collections.EMPTY_MAP;
        }
//        IMPLog("Context: %@", context);
        FeatureEncoder encoder = new FeatureEncoder(table, modelSeed);
        Map<Integer, Double> encodedContext = encoder.encodeFeatures(context);

        List<Map<Integer, Double>> result = new ArrayList<>(variants.size());
        for (Object variant : variants) {
            result.add(encoder.encodeFeatures(variant, encodedContext));
        }

        return result;
    }

    /**
     * Performs reservoir sampling to break ties when variants have the same score.
     *
     * @param variants
     * @param scores
     * @return
     */
    private Object findBestSample(List variants, List<Number> scores) {
        double bestScore = -Double.MAX_VALUE;
        Object bestVariant = null;
        int replacementCount = 0;
        Random random = new Random();
        for (int i = 0; i < scores.size(); ++i) {
            double score = scores.get(i).doubleValue();
            if (score > bestScore) {
                bestScore = score;
                bestVariant = variants.get(i);
                replacementCount = 0;
            } else if (score == bestScore) {
                double replacementProbability = 1.0 / (double) (2 + replacementCount);
                replacementCount++;
                if (random.nextDouble() <= replacementProbability) {
                    bestScore = score;
                    bestVariant = variants.get(i);
                }
            }
        }
        return bestVariant;
    }

}
