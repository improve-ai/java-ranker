package ai.improve;

import java.util.List;

public class IMPUtils {
    /**
     * If variants.size() != scores.size(), an IndexOutOfBoundException exception will be thrown
     * @return the variant with the best score. Null is returned if variants is empty.
     * */
    public static Object topScoringVariant(List<Object> variants, List<Double> scores) {
        if(variants == null || variants.size() <= 0) {
            return null;
        }
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

    public static boolean shouldtrackRunnersUp(int variantsCount, int maxRunnersUp) {
        if(variantsCount <= 1 || maxRunnersUp == 0) {
            return false;
        }
        return Math.random() < 1.0 / Math.min(variantsCount - 1, maxRunnersUp);
    }
}
