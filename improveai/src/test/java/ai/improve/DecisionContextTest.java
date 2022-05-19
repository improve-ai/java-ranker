package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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

    private Map givens() {
        return Map.of("lang", "en");
    }

    @Test
    public void testChooseFrom() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        Decision decision = decisionContext.chooseFrom(variants());
        assertNotNull(decision);
    }

    @Test
    public void testChooseFrom_generic() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        String greeting = decisionModel.given(null).chooseFrom(variants()).get();
        IMPLog.d(Tag, "greeting is " + greeting);
    }

    @Test
    public void testChooseFrom_null_variants() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.chooseFrom(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseFrom_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.chooseFrom(new ArrayList());
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
        assertEquals("hello", decision.best);
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
    public void testFirst() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.given(givens).first("hi", "hello", "hey");
        assertEquals("hi", first);
    }

    @Test
    public void testFirst_one_argument() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.given(givens).first(Arrays.asList("hi", "hello", "hey"));
        assertEquals("hi", first);
    }

    @Test
    public void testFirst_empty() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).first();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testFirst_empty_list() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).first(new ArrayList());
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
    public void testRandom() {
        int loop = 10000;
        Map<String, Integer> countMap = new HashMap<>();
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.setTrackURL(null);
        for(int i = 0; i < loop; ++i) {
            String variant = (String) decisionModel.given(null).random(variants);
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
    public void testRandom_one_argument() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Object first = decisionModel.given(givens).first(Arrays.asList("hi", "hello", "hey"));
        assertEquals("hi", first);
    }

    @Test
    public void testRandom_no_arguments() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).random();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testRandom_empty_list() {
        Map givens = Map.of("lang", "en");
        DecisionModel decisionModel = new DecisionModel("greetings");
        try {
            decisionModel.given(givens).random(new ArrayList());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseMultiVariate() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        variants.put("color", Arrays.asList("#000000", "#ffffff"));
        DecisionModel decisionModel = new DecisionModel("theme");
        decisionModel.given(null).chooseMultiVariate(variants);
    }

    @Test
    public void testChooseMultiVariate_null_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.chooseMultiVariate(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testChooseMultiVariate_empty_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.chooseMultiVariate(new HashMap<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_nil_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.which((Object)null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_empty_map() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.which(new HashMap<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_empty_list() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        try {
            decisionContext.which(new ArrayList<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testWhich_valid() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        Object best = decisionContext.which(1, 2, 3);
        assertNotNull(best);
    }

    @Test
    public void testScore_null_variants() {
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
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
