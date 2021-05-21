package ai.improve.android;


import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class BaseIMPDecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    @Test
    public void testLoad() {
    }

    @Test
    public void testRank() {
        int count = 100;
        List<Object> variants = new ArrayList();
        List<Double> scores = new ArrayList<>();

        for(int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add((double)i);
        }

        Random random = new Random();
        // shuffle
        for(int i = 0; i < 100; ++i) {
            int m = random.nextInt(count);
            int n = random.nextInt(count);
            Collections.swap(variants, m, n);
            Collections.swap(scores, m, n);
        }
        IMPLog.d(Tag, "Shuffled.....");
        for(int i = 0; i < variants.size(); ++i) {
            IMPLog.d(Tag, "" + variants.get(i));
        }

        IMPLog.d(Tag, "Sorted.....");
        List<Object> sorted = BaseIMPDecisionModel.rank(variants, scores);
        assertEquals(sorted.size(), variants.size());

        for(int i = 0; i < sorted.size(); ++i) {
            IMPLog.d(Tag, "" + sorted.get(i));
            if(i != variants.size()-1) {
                assertTrue((Integer)sorted.get(i) > (Integer) sorted.get(i+1));
            }
        }
    }

    @Test
    public void testTopVariant() {
        int count = 100;
        List<Object> variants = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        Random random = new Random();

        for (int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add(random.nextDouble());
        }
        variants.add(count+1000);
        scores.add(10.0);

        int m = random.nextInt(count);
        Collections.swap(variants, m, count);
        Collections.swap(scores, m, count);

        Integer topVariant = (Integer) BaseIMPDecisionModel.topScoringVariant(variants, scores);
        IMPLog.d(Tag, "topVariant=" + topVariant.intValue());
        assertEquals(topVariant.intValue(), count+1000);
    }

    @Test
    public void testTopVariantEmpty() {
        List<Object> variants = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        Integer topVariant = (Integer) BaseIMPDecisionModel.topScoringVariant(variants, scores);
        assertNull(topVariant);
    }

    @Test
    public void testTopVariantInvalid() {
        List<Object> variants = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        int count = 10;
        Random random = new Random();
        for (int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add(random.nextDouble());
        }
        variants.add(count);

        Integer topVariant = (Integer) BaseIMPDecisionModel.topScoringVariant(variants, scores);
        assertNull(topVariant);
    }
}