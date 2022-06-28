package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static ai.improve.DecisionTrackerTest.Track_Api_Key;
import static ai.improve.DecisionTrackerTest.Track_URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ai.improve.log.IMPLog;
import ai.improve.provider.GivensProvider;
import ai.improve.util.ModelUtils;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class DecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    public static final String ModelURL = "https://improveai-mindblown-mindful-prod-models.s3.amazonaws.com/models/latest/improveai-songs-2.0.xgb.gz";

    public static final String DefaultFailMessage = "A runtime exception should have been thrown, we should never have reached here";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
        DecisionModel.setDefaultTrackApiKey(Track_Api_Key);
    }

    private List<String> variants() {
        return Arrays.asList("Hello", "Hi", "Hey");
    }

    private List<Double> scores() {
        return Arrays.asList(0.1, 0.2, 0.3);
    }

    @BeforeEach
    public void setUp() throws Exception {
        IMPLog.d(Tag, "setUp");
    }

    private class AlphaGivensProvider implements GivensProvider {
        @Override
        public Map<String, Object> givensForModel(DecisionModel decisionModel, Map<String, Object> givens) {
            return null;
        }
    }

    @Test
    public void testModelName_null() {
        try {
            new DecisionModel(null);
        } catch (Exception e) {
            return ;
        }
        fail(DefaultFailMessage);
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
                count++;
            }
        }
        assertEquals(l.size(), count);
    }

    @Test
    public void testSetTrackURL_Null() {
        DecisionModel decisionModel = new DecisionModel("hello");
        assertNotNull(decisionModel.getTrackURL());
        assertNotNull(decisionModel.getTracker());

        decisionModel.setTrackURL(null);
        assertNull(decisionModel.getTrackURL());
        assertNull(decisionModel.getTracker());
    }

    @Test
    public void testSetTrackURL_Empty() {
        DecisionModel decisionModel = new DecisionModel("hello");
        assertNotNull(decisionModel.getTrackURL());
        assertNotNull(decisionModel.getTracker());

        try {
            decisionModel.setTrackURL("");
        } catch (Exception e) {
            IMPLog.e(Tag, e.getMessage());
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testSetTrackURL_Valid() {
        DecisionModel decisionModel = new DecisionModel("hello", null, null);
        assertNull(decisionModel.getTrackURL());
        assertNull(decisionModel.getTracker());
        assertNull(decisionModel.getTrackApiKey());

        decisionModel.setTrackURL(Track_URL);
        assertEquals(Track_URL, decisionModel.getTrackURL());
        assertNotNull(decisionModel.getTracker());
    }

    @Test
    public void testConstructor_default_url_and_api_key() {
        DecisionModel decisionModel = new DecisionModel("hello");
        assertEquals(Track_URL, decisionModel.getTrackURL());
        assertEquals(Track_Api_Key, decisionModel.getTrackApiKey());
        assertEquals(Track_Api_Key, decisionModel.getTracker().getTrackApiKey());

        decisionModel.setTrackApiKey(null);
        assertNull(decisionModel.getTrackApiKey());
        assertNull(decisionModel.getTracker().getTrackApiKey());

        decisionModel.setTrackURL(null);
        assertNull(decisionModel.getTrackURL());
        assertNull(decisionModel.getTracker());
    }

    @Test
    public void testTrackURL_Null() {
        DecisionModel decisionModel = new DecisionModel("hello", null, null);
        assertNull(decisionModel.getTrackURL());
        assertNull(decisionModel.getTracker());
    }

    @Test
    public void testTrackURL_Invalid() {
        try {
            new DecisionModel("hello", "", null);
        } catch (Exception e) {
            IMPLog.d(Tag, e.getMessage());
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testDefaultTrackURL() {
        DecisionModel.setDefaultTrackURL(null);

        assertNull(DecisionModel.getDefaultTrackURL());
        DecisionModel decisionModel = new DecisionModel("hello");
        assertNull(decisionModel.getTrackURL());

        DecisionModel.setDefaultTrackURL(Track_URL);

        decisionModel = new DecisionModel("hello");
        assertNotNull(decisionModel.getTrackURL());
        assertEquals(Track_URL, decisionModel.getTrackURL());
    }

    @Test
    public void testSetDefaultTrackURL() {
        DecisionModel.setDefaultTrackURL(null);
        assertNull(DecisionModel.getDefaultTrackURL());

        try {
            DecisionModel.setDefaultTrackURL("");
        } catch (Exception e) {
            return;
        }
        fail(DefaultFailMessage);
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
    public void testRank_null_variants() {
        try {
            DecisionModel.rank(null, Arrays.asList(0.1, 0.2));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRank_null_scores() {
        try {
            DecisionModel.rank(Arrays.asList("hi", "hello"), null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
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
        } catch (IllegalArgumentException e) {
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
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail("An IndexOutOfBoundException should have been thrown, we should never reach here");
    }

    @Test
    public void testRank_generic() {
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        List<Double> scores = Arrays.asList(0.1, 0.2, 0.3);
        // test that we don't have to do type cast here like:
        // String greeting = (String) DecisionModel.rank(variants, scores).get(0);
        String greeting = DecisionModel.rank(variants, scores).get(0);
        assertEquals("hey", greeting);
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
        Map<String, Object> givens = new HashMap<>();
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.given(givens);

        decisionModel.given(Map.of("lang", "en"));
        decisionModel.given(Map.of("size", 1));
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
    public void testChooseFrom_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.chooseFrom(variants()).get();
        IMPLog.d(Tag, "greetings is " + greeting);
    }

    @Test
    public void testChooseFromVariantsAndScores() {
        List variants = Arrays.asList("hi", "hello", "Hey");
        List scores = Arrays.asList(0.1, 1.0, -0.1);
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.chooseFrom(variants, scores);
        assertEquals("hello", decision.best);
        assertNull(decision.givens);
        assertEquals(scores, decision.scores);
        assertEquals(variants, decision.variants);
    }

    @Test
    public void testChooseFromVariantsAndScores_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.chooseFrom(variants(), scores()).get();
        IMPLog.d(Tag, "greetings is " + greeting);
    }

    @Test
    public void testChooseFromVariantsAndScores_empty_variants() {
        List variants = new ArrayList();
        List scores = Arrays.asList(0.1, 1.0, -0.1);
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseFrom(variants, scores);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromVariantsAndScores_null_variants() {
        List variants = null;
        List scores = Arrays.asList(0.1, 1.0, -0.1);
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseFrom(variants, scores);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromVariantsAndScores_size_not_equal() {
        List variants = Arrays.asList("hi", "hello", "Hey");
        List scores = Arrays.asList(0.1, 1.0);
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseFrom(variants, scores);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseMultiVariate_null_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.chooseMultiVariate(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseMultiVariate_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.chooseMultiVariate(new HashMap<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseMultiVariate_1_variant() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseMultiVariate(variants);
        List expected = Arrays.asList(
                new HashMap<String, String>(){{
                    put("font", "Italic");
                }},
                new HashMap<String, String>(){{
                    put("font", "Bold");
                }});
        assertTrue(expected.equals(decision.variants));
    }

    @Test
    public void testChooseMultiVariate_2_variants() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        variants.put("color", Arrays.asList("#000000", "#ffffff"));
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseMultiVariate(variants);
        List expected = Arrays.asList(
                new HashMap<String, String>(){{
                    put("font", "Italic");
                    put("color", "#000000");
                }},
                new HashMap<String, String>(){{
                    put("font", "Italic");
                    put("color", "#ffffff");
                }},
                new HashMap<String, String>(){{
                    put("font", "Bold");
                    put("color", "#000000");
                }},
                new HashMap<String, String>(){{
                    put("font", "Bold");
                    put("color", "#ffffff");
                }});
        assertEquals(expected, decision.variants);
    }

    @Test
    public void testChooseMultiVariate_3_variants() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        variants.put("color", Arrays.asList("#000000", "#ffffff"));
        variants.put("size", 3);
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseMultiVariate(variants);
        List expected = Arrays.asList(
                new HashMap<String, Object>(){{
                    put("font", "Italic");
                    put("color", "#000000");
                    put("size", 3);
                }},
                new HashMap<String, Object>(){{
                    put("font", "Italic");
                    put("color", "#ffffff");
                    put("size", 3);
                }},
                new HashMap<String, Object>(){{
                    put("font", "Bold");
                    put("color", "#000000");
                    put("size", 3);
                }},
                new HashMap<String, Object>(){{
                    put("font", "Bold");
                    put("color", "#ffffff");
                    put("size", 3);
                }});
        assertEquals(expected, decision.variants);
    }

    @Test
    public void testWhich_null_argument() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.which((Object)null);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_no_argument() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.which();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_empty_map() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.which(new HashMap<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_empty_list() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.which(new ArrayList<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }


    @Test
    public void testWhich_1_argument_non_array() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.which(1);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_1_argument_array() {
        DecisionModel decisionModel = new DecisionModel("theme");
        decisionModel.which(Arrays.asList(1));
    }

    @Test
    public void testWhich_1_argument_map() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic"));
        variants.put("color", Arrays.asList("#000000"));
        variants.put("size", 3);
        DecisionModel decisionModel = new DecisionModel("theme");
        Object best = decisionModel.which(variants);
        assertEquals(new HashMap<String, Object>(){{
            put("font", "Italic");
            put("color", "#000000");
            put("size", 3);
        }}, best);
    }

    @Test
    public void testWhich_multiple_arguments() {
        DecisionModel decisionModel = new DecisionModel("theme");
        Object best = decisionModel.which(Arrays.asList(1, 2, 3), 2, 3, "hello");
        IMPLog.d(Tag, "best is " + best);
    }

    @Test
    public void testChooseFirst() {
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.chooseFirst(variants);
        assertEquals("hi", decision.best);
        assertEquals(variants, decision.variants);
        assertNull(decision.givens);
        assertEquals(variants.size(), decision.scores.size());
    }

    @Test
    public void testChooseFirst_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.chooseFirst(variants()).get();
        IMPLog.d(Tag, "greetings is " + greeting);
    }

    @Test
    public void testChooseFirst_null_variants() {
        List variants = null;
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseFirst(variants);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFirst_empty_variants() {
        List variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseFirst(variants);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirst() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.first("hi", "hello", "hey");
        assertEquals("hi", first);
    }

    @Test
    public void testFirst_empty() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.first();
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirst_null() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.first(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirst_one_argument() {
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.first((Object) variants);
        assertEquals("hi", first);
    }

    @Test
    public void testFirst_one_argument_empty_list() {
        List variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.first((Object) variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirst_one_argument_not_list() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.first("hi");
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseRandom() {
        int loop = 100000;
        Map<String, Integer> countMap = new HashMap<>();
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        for(int i = 0; i < loop; ++i) {
            String variant = (String) decisionModel.chooseRandom(variants).get();
            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }
        IMPLog.d(Tag, "count: " + countMap);
        assertEquals(loop/3, countMap.get("hello"), loop/3.0*0.03);
        assertEquals(loop/3, countMap.get("hi"), loop/3.0*0.03);
        assertEquals(loop/3, countMap.get("hey"), loop/3.0*0.03);
    }

    @Test
    public void testChooseRandom_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.chooseRandom(variants()).get();
        IMPLog.d(Tag, "greetings is " + greeting);
    }

    @Test
    public void testChooseRandom_empty_variants() {
        List variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseRandom(variants);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseRandom_null_variants() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseRandom(null);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandom() {
        int loop = 100000;
        Map<String, Integer> countMap = new HashMap<>();
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        for(int i = 0; i < loop; ++i) {
            String variant = (String) decisionModel.random("hi", "hello", "hey");
            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }
        IMPLog.d(Tag, "count: " + countMap);
        assertEquals(loop/3, countMap.get("hello"), loop/3.0*0.03);
        assertEquals(loop/3, countMap.get("hi"), loop/3.0*0.03);
        assertEquals(loop/3, countMap.get("hey"), loop/3.0*0.03);
    }

    @Test
    public void testRandom_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.random();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandom_null_variants() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.random(null);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandom_one_argument() {
        int loop = 100000;
        List variants = Arrays.asList("hi", "hello", "hey");
        Map<String, Integer> countMap = new HashMap<>();
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        for(int i = 0; i < loop; ++i) {
            String variant = (String) decisionModel.random(variants);
            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }
        IMPLog.d(Tag, "count: " + countMap);
        assertEquals(loop/3, countMap.get("hello"), loop/3.0*0.03);
        assertEquals(loop/3, countMap.get("hi"), loop/3.0*0.03);
        assertEquals(loop/3, countMap.get("hey"), loop/3.0*0.03);
    }

    @Test
    public void testRandom_one_argument_empty_list() {
        List variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.random(variants);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testScore_without_loading_model() {
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

    @Test
    public void testAddReward_non_Android() {
        DecisionModel decisionModel = new DecisionModel("hello");
        try {
            decisionModel.addReward(0.1);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testGivensProvider_getter_setter() {
        DecisionModel decisionModel = new DecisionModel("hello");
        assertNull(decisionModel.getGivensProvider());

        GivensProvider givensProvider = new AlphaGivensProvider();

        decisionModel.setGivensProvider(givensProvider);
        assertNotNull(decisionModel.getGivensProvider());
        assertEquals(givensProvider, decisionModel.getGivensProvider());

        decisionModel.setGivensProvider(null);
        assertNull(decisionModel.getGivensProvider());

        DecisionModel.setDefaultGivensProvider(new AlphaGivensProvider());
        assertNotNull(decisionModel.getGivensProvider());

        assertNotNull(new DecisionModel("hello").getGivensProvider());
    }
}