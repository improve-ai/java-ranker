package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

import static ai.improve.DecisionModelTest.DefaultFailMessage;
import static ai.improve.DecisionTrackerTest.Track_URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DecisionContextTest {
    public static final String Tag = "DecisionContextTest";

    @BeforeAll
    public static void setUp() {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
    }

    private List<String> variants() {
        return Arrays.asList("Hello", "Hi", "Hey");
    }

    private List<Double> scores() {
        return Arrays.asList(0.1, 0.2, 0.3);
    }

    private Map<String, String> givens() {
        return Map.of("lang", "en");
    }

    private DecisionModel model() {
        return new DecisionModel("greetings");
    }

    @Test
    public void testDecide() {
        DecisionContext decisionContext = model().given(givens());

        Decision<String> decision = decisionContext.decide(Arrays.asList("Hi", "Hello", "Hey"));
        assertEquals("Hi", decision.get());
        assertEquals(3, decision.ranked().size());

        String greeting = decisionContext.decide(Arrays.asList("Hi", "Hello", "Hey")).get();
        int size = decisionContext.decide(Arrays.asList(1, 2, 3)).get();
        IMPLog.d(Tag, "greeting = " + greeting + ", size = " + size);
    }

    @Test
    public void testDecide_null_variants() {
        try {
            model().given(givens()).decide(null);
            fail("variants can't be nil");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDecide_empty_variants() {
        try {
            model().given(givens()).decide(new ArrayList());
            fail("variants can't be empty");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDecide_false() {
        List<String> variants = variants();
        String chosen = model().given(givens()).decide(variants, false).get();
        assertEquals(variants.get(0), chosen);
    }

    @Test
    public void testDecideWithScore() {
        List<String> variants = Arrays.asList("Hi", "Hello", "Hey");
        List<Double> scores = Arrays.asList(1.1, 3.3, 2.2);
        Decision<String> decision = model().given(givens()).decide(variants, scores);
        assertEquals("Hello", decision.get());
        assertEquals(Arrays.asList("Hello", "Hey", "Hi"), decision.ranked());
    }

    @Test
    public void testChooseFrom() {
        List<String> variants = variants();
        Decision<String> decision = model().given(givens()).chooseFrom(variants());
        assertEquals(variants.get(0), decision.get());
    }

    @Test
    public void testChooseFrom_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = model().given(givens()).chooseFrom(variants()).get();
        IMPLog.d(Tag, "greeting is " + greeting);
    }

    @Test
    public void testChooseFrom_null_variants() {
        try {
            model().given(givens()).chooseFrom(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFrom_empty_variants() {
        try {
            model().given(givens()).chooseFrom(new ArrayList());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromVariantsAndScores() {
        Map givens = Map.of("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        List scores = Arrays.asList(0.05, 0.1, 0.08);
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFrom(variants, scores);
        assertEquals("hello", decision.get());
        assertEquals(1, decision.givens.size());
        assertEquals("en", decision.givens.get("lang"));
    }

    @Test
    public void testChooseFromVariantsAndScores_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.given(null).chooseFrom(variants(), scores()).get();
        IMPLog.d(Tag, "greeting is " + greeting);
    }

    @Test
    public void testChooseFromVariantsAndScores_null_variants() {
        Map givens = Map.of("lang", "en");
        List scores = Arrays.asList(1, 2, 3);
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).chooseFrom(null, scores);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromVariantsAndScores_empty_variants() {
        Map givens = Map.of("lang", "en");
        List scores = Arrays.asList(1, 2, 3);
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens()).chooseFrom(null, scores);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFromVariantsAndScores_invalid_size() {
        Map givens = Map.of("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        List scores = Arrays.asList(0.05, 0.1, 0.08, 0.09);
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).chooseFrom(variants, scores);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFirst() {
        Map givens = Map.of("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFirst(variants);
        assertEquals(1, decision.givens.size());
        assertEquals("en", decision.givens.get("lang"));
        assertEquals("hi", decision.get());
    }

    @Test
    public void testChooseFirst_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.given(null).chooseFirst(variants()).get();
        IMPLog.d(Tag, "greeting is " + greeting);
    }

    @Test
    public void testChooseFirst_null_variants() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).chooseFirst(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFirst_empty_variants() {
        Map givens = Map.of("lang", "en");
        List variants = new ArrayList();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).chooseFirst(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirstVariadic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.given(givens()).first("hi", "hello", "hey");
        assertEquals("hi", first);
    }

    @Test
    public void testFirstVariadic_null() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.given(givens()).first((String)null);
        assertNull(greeting);
    }

    @Test
    public void testFirstVariadic_empty() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens()).first();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirstList() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String first = decisionModel.given(givens()).first(Arrays.asList("hi", "hello", "hey"));
        assertEquals("hi", first);
    }

    @Test
    public void testFirstList_null() {
        List<String> variants = null;
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens()).first(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirstList_empty() {
        List<String> variants = new ArrayList<>();
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens()).first(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseRandom() {
        int loop = 10000;
        Map<String, Integer> countMap = new HashMap<>();
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        for(int i = 0; i < loop; ++i) {
            String variant = (String) decisionModel.given(null).chooseRandom(variants).get();
            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }
        IMPLog.d(Tag, "count: " + countMap);
        assertEquals(loop/3, countMap.get("hello"), 150);
        assertEquals(loop/3, countMap.get("hi"), 150);
        assertEquals(loop/3, countMap.get("hey"), 150);
    }

    @Test
    public void testChooseRandom_generic() {
        Map givens = Map.of("lang", "en");
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.given(givens).chooseRandom(variants).get();
        IMPLog.d(Tag, "greeting is " + greeting);
    }

    @Test
    public void testChooseRandom_null_variants() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(null).chooseRandom(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseRandom_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(null).chooseRandom(new ArrayList());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandomVariadic() {
        int loop = 10000;
        Map<String, Integer> countMap = new HashMap<>();
        List<String> variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        for(int i = 0; i < loop; ++i) {
            String variant = decisionModel.given(null).random(variants);
            if(countMap.containsKey(variant)) {
                countMap.put(variant, countMap.get(variant) + 1);
            } else {
                countMap.put(variant, 1);
            }
        }
        IMPLog.d(Tag, "count: " + countMap);
        assertEquals(loop/3, countMap.get("hello"), 150);
        assertEquals(loop/3, countMap.get("hi"), 150);
        assertEquals(loop/3, countMap.get("hey"), 150);
    }

    @Test
    public void testRandomVariadic_null() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.given(givens()).random((String)null);
        assertNull(greeting);
    }

    @Test
    public void testRandomVariadic_empty() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens()).random();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandomList() {
        List<String> variants = Arrays.asList("Hi", "Hello", "Hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.given(givens()).random(variants);
    }

    @Test
    public void testRandomList_empty() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens()).random(new ArrayList());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandomList_null() {
        List<String> variants = null;
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens()).random(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testOptimize() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        variants.put("color", Arrays.asList("#000000", "#ffffff"));
        DecisionModel decisionModel = new DecisionModel("theme");
        decisionModel.given(null).optimize(variants);
    }

    @Test
    public void testOptimize_null_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.optimize(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testOptimize_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.optimize(new HashMap<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = decisionModel.given(null);
        int size = decisionContext.which(1, 2, 3);
        assertEquals(1, size);

        String greeting = decisionModel.which("Hi", "Hello", "Hey");
        assertEquals("Hi", greeting);
    }

    @Test
    public void testWhich_null() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        String greeting = decisionContext.which((String)null);
        assertNull(greeting);
    }

    @Test
    public void testWhich_empty() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.which();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhichFrom() {
        List<String> variants = Arrays.asList("Hello", "Hi", "Hey");
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = decisionModel.given(null);
        String greeting = decisionContext.whichFrom(variants);
        assertEquals("Hello", greeting);
    }

    @Test
    public void testWhichFrom_null() {
        List<String> variants = null;
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = decisionModel.given(null);
        try {
            decisionContext.whichFrom(variants);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhichFrom_empty() {
        List<String> variants = new ArrayList<>();
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = decisionModel.given(null);
        try {
            decisionContext.whichFrom(variants);
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
    public void testScore_null_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = decisionModel.given(null);
        try {
            decisionContext.score(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testScore_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.score(new ArrayList<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testScore_valid() {
        List variants = Arrays.asList(1, 2, 3);
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        List<Double> scores = decisionContext.score(variants);
        assertEquals(variants.size(), scores.size());
    }
}
