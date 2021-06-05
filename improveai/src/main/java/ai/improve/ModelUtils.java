package ai.improve;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ModelUtils {

    private static Random randomGenerator = new Random();

    /**
     * If variants.size() != scores.size(), an IndexOutOfBoundException exception will be thrown
     * @return the variant with the best score. Null is returned if variants is empty.
     * */
    public static <T> T topScoringVariant(List<T> variants, List<Double> scores) {
        if(variants == null || variants.size() <= 0) {
            return null;
        }
        // check the size of variants and scores, and use the bigger one so that
        // an IndexOutOfBoundOfException would be thrown later
        int size = variants.size();
        if(scores.size() > variants.size()) {
            size = scores.size();
        }

        T topVariant = variants.get(0);
        double bestScore = scores.get(0).doubleValue();
        for(int i = 1; i < size; ++i) {
            // scores.get(i) and variants.get(i) must be called for each loop.
            // We are relying on this to trigger an IndexOutOfBoundExeception
            // when variants.size() != scores.size()
            double score = scores.get(i).doubleValue();
            T variant = variants.get(i);
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

    // Generate n = variants.count random (double) gaussian numbers
    // Sort the numbers descending and return the sorted list
    // The median value of the list is expected to have a score near zero
    public static List<Double> generateDescendingGaussians(int count) {
        Double[] scores = new Double[count];
        for (int i = 0; i < count; ++i) {
            scores[i] = randomGenerator.nextGaussian();
        }
        Arrays.sort(scores, Collections.reverseOrder());
        return Arrays.asList(scores);
    }
}
