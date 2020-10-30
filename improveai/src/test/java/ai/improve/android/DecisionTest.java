package ai.improve.android;


import ai.improve.android.spi.DecisionMaker;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DecisionTest {

    @Test
    public void testGenerateScores() {
        List<Object> variants = generateVariants();
        Decision d = new DecisionMaker(variants, "test_model");
        List<? extends Number> scores = d.scores();
        System.out.println(scores);
    }

    private List<Object> generateVariants() {
        List<Object> result = new ArrayList();
        for(int i = 0; i < 1000; ++i) {
            result.add(RandomStringUtils.random(100));
        }
        return result;
    }
}
