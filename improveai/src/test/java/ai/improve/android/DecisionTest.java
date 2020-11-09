package ai.improve.android;


import ai.improve.android.spi.DecisionMaker;
import junit.framework.Assert;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import javax.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.List;

public class DecisionTest {


    @Test
    public void testScores() {
        List<Object> variants = generateVariants();
        Decision d = new DecisionMaker(variants, "test_model");
        List scores = d.scores();

        try {
            scores.add(new Integer(1000));
            Assert.fail("Scores should be immutable");
        } catch (UnsupportedOperationException ex) {
            //this is expected
        }

        List scores2 = d.scores();

        //Ensuring we have exactly the same object from Decision class
        Assert.assertTrue(scores == scores2);
    }

    @Test
    public void testRanked() {
        List<Object> variants = generateVariants();
        Decision d = new DecisionMaker(variants, "test_model");
        List results = d.ranked();

        Assert.assertEquals(variants, results);
        try {
            results.add(new Integer(1000));
            Assert.fail("Ranked should be immutable");
        } catch (UnsupportedOperationException ex) {
            //this is expected
        }

        List results2 = d.ranked();

        //Ensuring we have exactly the same object from Decision class
        Assert.assertTrue(results == results2);
    }

    @Test
    public void testScored() {
        List<Object> variants = generateVariants();
        Decision d = new DecisionMaker(variants, "test_model");
        List results = d.scored();
        try {
            results.add(new Integer(1000));
            Assert.fail("Scored should be immutable");
        } catch (UnsupportedOperationException ex) {
            //this is expected
        }

        List results2 = d.scored();

        //Ensuring we have exactly the same object from Decision class
        Assert.assertTrue(results == results2);
    }

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
