package ai.improve.android;


import ai.improve.android.spi.DecisionMaker;
import ai.improve.android.spi.DefaultDecisionModel;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import junit.framework.Assert;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;


@RunWith(RobolectricTestRunner.class)
public class DecisionTestWithModel {

    @Test
    public void testGenerateScores() throws Exception {

        ImprovePredictor p = new ImprovePredictor(getClass().getResourceAsStream("/model_appended.xgb"));
        //System.out.println(p.getModelMetadata().getUserDefinedMetadata());
        DecisionModel model = DefaultDecisionModel.initWithModel(p);

        List<Object> variants = generateVariants();

        Decision d = new DecisionMaker(variants, model);
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
