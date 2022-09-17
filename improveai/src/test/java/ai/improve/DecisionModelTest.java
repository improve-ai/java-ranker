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

    private DecisionModel model() {
        return new DecisionModel("greetings");
    }

    @BeforeEach
    public void setUp() throws Exception {
        IMPLog.d(Tag, "setUp");
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
        DecisionModel.setDefaultTrackApiKey(Track_Api_Key);
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
    public void testGet() {
        DecisionModel decisionModel = DecisionModel.get("greetings");
        assertNotNull(decisionModel);
    }

    @Test
    public void testGet_invalid_model_name() {
        try {
            DecisionModel.get("");
            fail("model name can't be empty");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        try {
            DecisionModel.get(null);
            fail("model name can't be null");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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
    public void testRankWithScores() {
        int count = 100;
        List<Integer> variants = new ArrayList();
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
        List<Integer> sorted = DecisionModel.rank(variants, scores);
        assertEquals(sorted.size(), variants.size());

        for(int i = 0; i < sorted.size(); ++i) {
            IMPLog.d(Tag, "" + sorted.get(i));
            if(i != variants.size()-1) {
                assertTrue((Integer)sorted.get(i) > (Integer) sorted.get(i+1));
            }
        }
    }

    @Test
    public void testRankWithScores_null_variants() {
        try {
            DecisionModel.rank(null, Arrays.asList(0.1, 0.2));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRankWithScores_null_scores() {
        try {
            DecisionModel.rank(Arrays.asList("hi", "hello"), null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRankWithScores_largerVariants() {
        try {
            DecisionModel.rank(Arrays.asList("Hi", "Hello", "Hey"), Arrays.asList(1.1, 2.2));
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail("variants.size() must be equal to scores.size()");
    }

    @Test
    public void testRankWithScores_largerScores() {
        try {
            DecisionModel.rank(Arrays.asList("Hi", "Hello"), Arrays.asList(1.1, 2.2, 3.3));
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail("variants.size() must be equal to scores.size()");
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
    public void testDecide() {
        DecisionModel decisionModel = model();

        Decision<String> decision = decisionModel.decide(Arrays.asList("Hi", "Hello", "Hey"));
        assertEquals("Hi", decision.get());
        assertEquals(3, decision.ranked().size());

        String greeting = decisionModel.decide(Arrays.asList("Hi", "Hello", "Hey")).get();
        int size = decisionModel.decide(Arrays.asList(1, 2, 3)).get();
        IMPLog.d(Tag, "greeting = " + greeting + ", size = " + size);
    }

    @Test
    public void testDecide_false() {
        model().decide(variants(), false).get();
    }

    @Test
    public void testDecideWithScore() {
        List<String> variants = Arrays.asList("Hi", "Hello", "Hey");
        List<Double> scores = Arrays.asList(1.1, 3.3, 2.2);
        Decision<String> decision = model().decide(variants, scores);
        assertEquals("Hello", decision.get());
        assertEquals(Arrays.asList("Hello", "Hey", "Hi"), decision.ranked());
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
    }

    @Test
    public void testChooseFrom_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.chooseFrom(variants()).get();
        IMPLog.d(Tag, "greetings is " + greeting);
    }

    @Test
    public void testChooseFrom_empty() {
        List<String> variants = new ArrayList<>();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseFrom(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChoooseFrom_null() {
        List<String> variants = null;
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.chooseFrom(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromVariantsAndScores() {
        List<String> variants = Arrays.asList("hi", "hello", "Hey");
        List<Double> scores = Arrays.asList(0.1, 1.0, -0.1);
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision<String> decision = decisionModel.chooseFrom(variants, scores);
        assertEquals("hello", decision.get());
        assertNull(decision.givens);
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
        List<String> variants = Arrays.asList("hi", "hello", "Hey");
        List<Double> scores = Arrays.asList(0.1, 1.0);
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
    public void testChooseMultivariate_null_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.chooseMultivariate(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseMultivariate_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.chooseMultivariate(new HashMap<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseMultivariate_1_variant() {
        Map<String, List<String>> variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseMultivariate(variants);
        assertEquals(Arrays.asList(Map.of("font", "Italic"), Map.of("font", "Bold")), decision.ranked());
    }

    @Test
    public void testChooseMultivariate_2_variants() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        variants.put("color", Arrays.asList("#000000", "#ffffff"));
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseMultivariate(variants);
        assertEquals(Arrays.asList(
                Map.of("font", "Italic", "color", "#000000"),
                Map.of("font", "Italic", "color", "#ffffff"),
                Map.of("font", "Bold", "color", "#000000"),
                Map.of("font", "Bold", "color", "#ffffff")), decision.ranked());
    }

    @Test
    public void testChooseMultivariate_3_variants() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        variants.put("color", Arrays.asList("#000000", "#ffffff"));
        variants.put("size", 3);
        DecisionModel decisionModel = new DecisionModel("theme");
        Decision decision = decisionModel.chooseMultivariate(variants);
        assertEquals(Arrays.asList(
                Map.of("font", "Italic", "color", "#000000", "size", 3),
                Map.of("font", "Italic", "color", "#ffffff", "size", 3),
                Map.of("font", "Bold", "color", "#000000", "size", 3),
                Map.of("font", "Bold", "color", "#ffffff", "size", 3)), decision.ranked());
    }

    @Test
    public void testOptimize() {
        Map<String, ?> variantMap = Map.of("font", Arrays.asList("Italic", "Bold"), "size", Arrays.asList(12, 13));
        DecisionModel decisionModel = new DecisionModel("theme");
        Map<String, Object> theme = decisionModel.optimize(variantMap);
        assertEquals(2, theme.size());
        assertNotNull(theme.get("font"));
        assertNotNull(theme.get("size"));
    }

    @Test
    public void testOptimize_null_variants() {
        try {
            model().optimize(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testOptimize_empty_member() {
        Map<String, ?> variantMap = Map.of("font", Arrays.asList("Italic", "Bold"),
                "size", Arrays.asList(12, 13),
                "color", new ArrayList<>());
        Map<String, Object> theme = model().optimize(variantMap);
        assertEquals(2, theme.size());
        assertNotNull(theme.get("font"));
        assertNotNull(theme.get("size"));
    }

    @Test
    public void testOptimize_all_empty() {
        Map variants = new HashMap();
        variants.put("color", new ArrayList<String>());
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.optimize(variants);
            fail(DefaultFailMessage);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWhichVariadic() {
        DecisionModel decisionModel = new DecisionModel("theme");

        int size = decisionModel.which(1, 2, 3);
        assertEquals(1, size);

        String greeting = decisionModel.which("hi", "hello", "hey");
        assertEquals("hi", greeting);
    }

    @Test
    public void testWhichVariadic_null() {
        DecisionModel decisionModel = new DecisionModel("theme");
        decisionModel.which((String)null);
    }

    @Test
    public void testWhichVariadic_empty() {
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
    public void testWhichVariadic_mixed_types() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object chosen = decisionModel.which("hi", 0, true, 1.2f);
        assertEquals("hi", chosen);

        chosen = decisionModel.which(false, "hi", 0, true, 1.2f);
        assertEquals(false, chosen);
    }

    @Test
    public void testWhichFrom() {
        List<String> variants = Arrays.asList("Hi", "Hello", "Hey");
        DecisionModel decisionModel = new DecisionModel("theme");
        String greeting = decisionModel.whichFrom(variants);
        assertEquals("Hi", greeting);
    }

    @Test
    public void testWhichFrom_empty() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.whichFrom(new ArrayList<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhichFrom_null() {
        DecisionModel decisionModel = new DecisionModel("theme");
        try {
            decisionModel.whichFrom(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRank() {
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        List<String> rankedVariants = model().rank(variants);
        assertEquals(variants.size(), rankedVariants.size());
        for(String variant : rankedVariants) {
            assertTrue(variants.contains(variant));
        }
    }

    @Test
    public void testRank_null_variants() {
        try {
            model().rank(null);
            fail("variants can't be null");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRank_empty_variants() {
        try {
            model().rank(new ArrayList<>());
            fail("variants can't be empty");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFullFactorialVariants() {
        List variants = model().fullFactorialVariants(Map.of(
                "color", Arrays.asList("black", "white"),
                "fontSize", Arrays.asList(12, 13),
                "style", "bold",
                "count", new ArrayList<>()));
        assertEquals(4, variants.size());
        List expectedVariants = Arrays.asList(
                Map.of("fontSize", 12, "color", "black", "style", "bold"),
                Map.of("fontSize", 12, "color", "white", "style", "bold"),
                Map.of("fontSize", 13, "color", "black", "style", "bold"),
                Map.of("fontSize", 13, "color", "white", "style", "bold"));
        for(int i = 0; i < variants.size(); ++i) {
            assertTrue(expectedVariants.contains(variants.get(i)));
        }
    }

    @Test
    public void testChooseFirst() {
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.chooseFirst(variants);
        assertEquals("hi", decision.get());
        assertEquals(variants, decision.ranked());
        assertNull(decision.givens);
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
    public void testFirstVariadic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String first = decisionModel.first("hi", "hello", "hey");
        assertEquals("hi", first);
    }

    @Test
    public void testFirstVariadic_mixed_types() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.first("hi", 0, true, 1.2f);
        assertEquals("hi", first);

        first = decisionModel.first(false, "hi", 0, true, 1.2f);
        assertEquals(false, first);
    }

    @Test
    public void testFirstVariadic_empty() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.first();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirstVariadic_null() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String chosen = decisionModel.first((String)null);
        assertNull(chosen);
    }

    @Test
    public void testFirstList() {
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        String first = decisionModel.first(variants);
        assertEquals("hi", first);
    }

    @Test
    public void testFirstList_empty() {
        List<String> variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.first(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirstList_null() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.first((List)null);
        } catch (IllegalArgumentException e) {
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
        } catch (IllegalArgumentException e) {
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
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandomVariadic() {
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
    public void testRandomVariadic_empty() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.random();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandomVariadic_null() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.random((String)null);
        assertNull(greeting);
    }

    @Test
    public void testRandomVariadic_mixed_types() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.random("hi", 0, true, 1.2f);
    }

    @Test
    public void testRandomList() {
        int loop = 100000;
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        Map<String, Integer> countMap = new HashMap<>();
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        for(int i = 0; i < loop; ++i) {
            String variant = decisionModel.random(variants);
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
    public void testRandomList_empty() {
        List<String> variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.random(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandomList_null() {
        List<String> variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.random(variants);
        } catch (IllegalArgumentException e) {
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
    public void testAddRewardForDecision() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.addReward(1.0, "abcd");
    }

    @Test
    public void testAddRewardForDecision_empty_decisionId() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.addReward(1.0, "");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddRewardForDecision_null_decisionId() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.addReward(1.0, null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddRewardForDecision_nan() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.addReward(Double.NaN, "abcd");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddRewardForDecision_positive_infinity() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.addReward(Double.POSITIVE_INFINITY, "abcd");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddRewardForDecision_negative_infinity() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.addReward(Double.POSITIVE_INFINITY, "abcd");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddRewardForDecision_null_trackURL() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        try {
            decisionModel.addReward(1.0, "abcd");
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