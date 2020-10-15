package ai.improve.android.chooser;

import ai.improve.android.hasher.FeatureEncoder;
import biz.k11i.xgboost.Predictor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ImproveChooser {

    private Predictor predictor;

    public ImproveChooser(Predictor p) {
        this.predictor = p;
    }

    public Object choose(List variants, Map<String, Object> context) {

//        NSArray *encodedFeatures = [self encodeVariants:variants withContext:context];
//        NSArray *scores = [self batchPrediction:encodedFeatures];

        List<Number> scores = Collections.singletonList(1.0);
        return findBestSample(variants, scores);
    }

    private Map encodeVariants(List variants, Map<String, Object> context)
    {
        if (context == null) {
            // Safe nil context handling
            context = Collections.EMPTY_MAP;
        }
//        IMPLog("Context: %@", context);
//        FeatureEncoder encoder = new FeatureEncoder();
//        encoder.encodeFeatures(context);

//        IMPFeatureHasher *hasher = [[IMPFeatureHasher alloc] initWithMetadata:self.metadata];
//        IMPFeaturesDictT *encodedContext = [hasher encodeFeatures:@{ @"context": context }];
//        IMPLog("Encoded context: %@", encodedContext);
//        NSMutableArray *encodedFeatures = [NSMutableArray arrayWithCapacity:variants.count];
//        for (NSDictionary *variant in variants) {
//        [encodedFeatures addObject:[hasher encodeFeatures:@{@"variant": variant}
//        startWith:encodedContext]];
//    }
//        return encodedFeatures;
        return Collections.emptyMap();
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
