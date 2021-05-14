package ai.improve.android;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class IMPDecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    @Test
    public void testLoad() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionModel model = IMPDecisionModel.loadFromAsset(appContext, "dummy_v6.xgb");
        assertNotNull(model);
    }

    @Test
    public void testRank() {
        int count = 100;
        List<Integer> variants = new ArrayList();
        List<Float> scores = new ArrayList<>();

        for(int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add((float)i);
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
        List<Integer> sorted = IMPDecisionModel.rank(variants, scores);
        assertEquals(sorted.size(), variants.size());

        for(int i = 0; i < sorted.size(); ++i) {
            IMPLog.d(Tag, "" + sorted.get(i));
            if(i != variants.size()-1) {
                assertTrue(sorted.get(i) > sorted.get(i+1));
            }
        }
    }

    @Test
    public void testTopVariant() {
        int count = 100;
        List<Integer> variants = new ArrayList<>();
        List<Number> scores = new ArrayList<>();

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

        Integer topVariant = IMPDecisionModel.topScoringVariant(variants, scores);
        IMPLog.d(Tag, "topVariant=" + topVariant.intValue());
        assertEquals(topVariant.intValue(), count+1000);
    }

    public void testTopVariantEmpty() {
        List<Integer> variants = new ArrayList<>();
        List<Number> scores = new ArrayList<>();

        Integer topVariant = IMPDecisionModel.topScoringVariant(variants, scores);
        assertNull(topVariant);
    }

    public void testTopVariantInvalid() {
        List<Integer> variants = new ArrayList<>();
        List<Number> scores = new ArrayList<>();

        int count = 10;
        Random random = new Random();
        for (int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add(random.nextDouble());
        }
        variants.add(count);

        Integer topVariant = IMPDecisionModel.topScoringVariant(variants, scores);
        assertNull(topVariant);
    }
}