package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static ai.improve.DecisionTrackerTest.Track_URL;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.improve.log.IMPLog;
import ai.improve.util.ModelUtils;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    public static final String DefaultFailMessage = "A runtime exception should have been thrown, we should never have reached here";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testModelName_null() throws IOException {
        // null is a valid model name
        new DecisionModel(null);
    }

    @Test
    public void testModelName_empty() {
        try {
            new DecisionModel("");
        } catch (RuntimeException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testModelName_Valid_Length() {
        new DecisionModel("a");
        new DecisionModel("abcde");

        String s = "";
        for(int i = 0; i < 64; ++i) {
            s += "a";
        }
        assertEquals(64, s.length());
        new DecisionModel(s);
    }

    @Test
    public void testModelName_Invalid_Length() {
        String s = "";

        int length = 65;
        for(int i = 0; i < length; ++i) {
            s += "a";
        }
        assertEquals(length, s.length());

        try {
            new DecisionModel(s);
        } catch (Exception e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testModelName_Valid_Character() {
        List<String> l = Arrays.asList(
                "a",
                "a_",
                "a.",
                "a-",
                "a1",
                "3abb"
        );
        assertTrue(l.size() > 0);

        for(int i = 0; i < l.size(); ++i) {
            new DecisionModel(l.get(i));
        }
    }

    @Test
    public void testModelName_Invalid_Character() {
        List<String> l = Arrays.asList(
                "_a",
                "a+",
                "a\\",
                ".a"
        );

        int count = 0;
        for(int i = 0; i < l.size(); ++i) {
            try {
                new DecisionModel(l.get(i));
            } catch (RuntimeException e) {
                e.printStackTrace();
                count++;
            }
        }
        assertEquals(l.size(), count);
    }

    @Test
    public void testDefaultTrackURL() throws MalformedURLException {
        assertNull(DecisionModel.defaultTrackURL);
        DecisionModel decisionModel = new DecisionModel("hello");
        assertNull(decisionModel.getTrackURL());

        DecisionModel.defaultTrackURL = Track_URL;

        decisionModel = new DecisionModel("hello");
        assertNotNull(decisionModel.getTrackURL());
        assertEquals(Track_URL, decisionModel.getTrackURL());
    }

    @Test
    public void testSetDefaultTrackURL() {

    }

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
        System.out.println("average=" + total/size);

        assertEquals(numbers.get(size/2), 0, 0.02);
        assertEquals(total/size, 0, 0.01);

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

    @Test
    public void testScoreWithoutLoadingModel() {
        int size = 100;

        List variants = new ArrayList();
        for(int i = 0; i < size; ++i) {
            variants.add(Math.random());
        }

        DecisionModel model = new DecisionModel("theme");
        List<Double> scores = model.score(variants);
        assertNotNull(scores);
        assertEquals(scores.size(), variants.size());

        // assert scores is in descending order
        for(int i = 0; i < scores.size()-1; ++i) {
            IMPLog.d(Tag, "score["+i+"] = " + scores.get(i));
            assertTrue(scores.get(i) > scores.get(i+1));
        }
    }
}