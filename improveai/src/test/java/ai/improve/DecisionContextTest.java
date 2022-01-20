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
    @BeforeAll
    public static void setUp() {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
    }

    private List variants() {
        return Arrays.asList("Hello", "Hi", "Hey");
    }

    @Test
    public void testChooseFrom() {
        DecisionModel decisionModel = new DecisionModel("greetings");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        Decision decision = decisionContext.chooseFrom(variants());
        assertNotNull(decision);
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
    public void testChooseMultiVariate() {
        Map variants = new HashMap();
        variants.put("font", Arrays.asList("Italic", "Bold"));
        variants.put("color", Arrays.asList("#000000", "#ffffff"));
        DecisionModel decisionModel = new DecisionModel("theme");
        DecisionContext decisionContext = new DecisionContext(decisionModel, null);
        decisionContext.chooseMultiVariate(variants);
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
