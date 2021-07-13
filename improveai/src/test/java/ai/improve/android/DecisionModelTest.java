package ai.improve.android;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.improve.DecisionModel;
import ai.improve.IMPLog;
import ai.improve.ModelUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DecisionModelTest {
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
        List<Object> sorted = DecisionModel.rank(variants, scores);
        assertEquals(sorted.size(), variants.size());

        for(int i = 0; i < sorted.size(); ++i) {
            IMPLog.d(Tag, "" + sorted.get(i));
            if(i != variants.size()-1) {
                assertTrue((Integer)sorted.get(i) > (Integer) sorted.get(i+1));
            }
        }
    }

    @Test
    public void testRankInvalid_largerVariants() {
        int count = 100;
        List<Object> variants = new ArrayList();
        List<Double> scores = new ArrayList<>();

        for(int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add((double)i);
        }
        variants.add(1);

        try {
            DecisionModel.rank(variants, scores);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return ;
        }
        fail("An IndexOutOfBoundException should have been thrown, we should never reach here");
    }

    @Test
    public void testRankInvalid_largerScores() {
        int count = 100;
        List<Object> variants = new ArrayList();
        List<Double> scores = new ArrayList<>();

        for(int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add((double)i);
        }
        scores.add(0.1);

        try {
            DecisionModel.rank(variants, scores);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return ;
        }
        fail("An IndexOutOfBoundException should have been thrown, we should never reach here");
    }

    @Test
    public void testTopScoringVariant() {
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

        Integer topVariant = (Integer) ModelUtils.topScoringVariant(variants, scores);
        IMPLog.d(Tag, "topVariant=" + topVariant.intValue());
        assertEquals(topVariant.intValue(), count+1000);
    }

    @Test
    public void testTopScoringVariantEmpty() {
        List<Object> variants = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        Integer topVariant = (Integer) ModelUtils.topScoringVariant(variants, scores);
        assertNull(topVariant);
    }

    // variants.lenth > scores.length
    @Test
    public void testTopScoringVariantInvalid_largerVariants() {
        List<Object> variants = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        int count = 10;
        Random random = new Random();
        for (int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add(random.nextDouble());
        }
        // Add one more variant, so that variants.lenght != scores.length
        variants.add(1);

        try {
            Integer topVariant = (Integer) ModelUtils.topScoringVariant(variants, scores);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail("An IndexOutOfBoundException should have been thrown, we should never reach here");
    }

    // variants.lenth < scores.length
    @Test
    public void testTopScoringVariantInvalid_largerScores() {
        List<Object> variants = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        int count = 10;
        Random random = new Random();
        for (int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add(random.nextDouble());
        }
        // Add one more variant, so that variants.lenght != scores.length
        scores.add(0.1);

        try {
            Integer topVariant = (Integer) ModelUtils.topScoringVariant(variants, scores);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail("An IndexOutOfBoundException should have been thrown, we should never reach here");
    }

    @Test
    public void testDescendingGaussians() throws Exception {
        int size = 100000;
        List<Double> numbers = ModelUtils.generateDescendingGaussians(size);
        assertEquals(numbers.size(), size);

        double total = 0.0;
        for(int i = 0; i < size; i++) {
            total += numbers.get(i);
        }

        System.out.println("median=" + numbers.get(size/2));
        System.out.println("averate=" + total/size);

        assertTrue(Math.abs(numbers.get(size/2)) < 0.01);
        assertTrue(Math.abs(total/size) < 0.01);

        // Test that it is descending
        for(int i = 0; i < size-1; ++i) {
            assertTrue(numbers.get(i) > numbers.get(i+1));
        }
    }

    @Test
    public void testGiven() {
        Map<String, Object> given = new HashMap<>();
        List<Object> variants = new ArrayList<>();
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.given(given).chooseFrom(variants);
    }

    @Test
    public void testChooseFrom() {
        DecisionModel decisionModel = new DecisionModel("music");

        // choose from double variants array
        decisionModel.chooseFrom(Arrays.asList(0.1, 0.2, 0.3)).get();

        // choose from string variants array
        decisionModel.chooseFrom(Arrays.asList("hello", "hi")).get();

        // choose from int variants list
        List<Integer> intVariants = new ArrayList<>();
        intVariants.add(1);
        intVariants.add(2);
        decisionModel.chooseFrom(intVariants).get();

        // choose from string variants list
        List<String> stringVariants = new ArrayList<>();
        stringVariants.add("hello");
        stringVariants.add("hi");
        decisionModel.chooseFrom(stringVariants).get();

        // Choose from complex objects
        // themeVariants = [ { "textColor": "#000000", "backgroundColor": "#ffffff" },
        // { "textColor": "#F0F0F0", "backgroundColor": "#aaaaaa" } ]
        List variants = Arrays.asList(
                new HashMap<String, String>(){{
                    put("textColor", "#000000");
                    put("backgroundColor", "#ffffff");
                }},
                new HashMap<String, String>() {{
                    put("textColor", "#F0F0F0");
                    put("backgroundColor", "#aaaaaa");
                }});
        decisionModel.chooseFrom(variants).get();


        // product = try DecisionModel.load(modelUrl).chooseFrom(["clutch", "dress", "jacket"]).get()
//        product = IMPDecisionModel.lo(modelUrl).chooseFrom(["clutch", "dress", "jacket"]).get()
    }
}