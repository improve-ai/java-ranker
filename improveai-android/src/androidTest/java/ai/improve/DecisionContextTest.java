package ai.improve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

@RunWith(AndroidJUnit4.class)
public class DecisionContextTest {
    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testChooseFrom() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        List variants = Arrays.asList("Hello", "Hi", "Hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFrom(variants);
        assertNotNull(decision);
        assertEquals("en", decision.givens.get("lang"));
        assertEquals(21, decision.givens.size());
    }

    @Test
    public void testChooseFirst() {
        Map givens = new HashMap();
        givens.put("lang", "en");
        List variants = Arrays.asList("hi", "hello", "hey");
        DecisionModel decisionModel = new DecisionModel("greetings");
        Decision decision = decisionModel.given(givens).chooseFirst(variants);
        assertEquals("en", decision.givens.get("lang"));
        assertEquals(21, decision.givens.size());
        assertEquals("hi", decision.get());
    }
}
