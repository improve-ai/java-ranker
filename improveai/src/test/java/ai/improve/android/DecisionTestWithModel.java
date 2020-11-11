package ai.improve.android;


import ai.improve.android.spi.DecisionMaker;
import ai.improve.android.spi.DefaultDecisionModel;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RunWith(RobolectricTestRunner.class)
public class DecisionTestWithModel {

    @Test
    public void testGenerateScores() throws Exception {

        ImprovePredictor p = new ImprovePredictor(getClass().getResourceAsStream("/model_w_metadata.xgb"));
        //System.out.println(p.getModelMetadata().getUserDefinedMetadata());
        DecisionModel model = DefaultDecisionModel.initWithModel(p);

        List<Object> variants = loadVariants("/datasets/2bs_bible_verses_full.json");

        List<Object> contextAsList = loadVariants("/datasets/context_sample_1.json");
        Map<String, Object> context = new HashMap<>();
        context.put("context", contextAsList);
        Decision d = new DecisionMaker(variants, model, context);
        List<? extends Number> scores = d.scores();
        System.out.println(scores);
        System.out.println(d.best());
        return;
    }

    private List<Object> loadVariants(String resource) throws Exception {
        String json = IOUtils.toString(getClass().getResourceAsStream(resource), "UTF-8");
        JSONArray arr = new JSONArray(json);
        List<Object> result = new ArrayList();
        for(int i = 0; i < arr.length(); ++i) {
            result.add(arr.getJSONObject(i));
        }
        return result;
    }
}
