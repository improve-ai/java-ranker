package ai.improve.android;

import ai.improve.android.spi.DecisionMaker;
import ai.improve.android.spi.DefaultDecisionModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class ModelWithUrlTest {

    private static final String MODEL_URL = "https://improve-v5-resources-prod-models-117097735164.s3-us-west-2.amazonaws.com/models/mindful/latest/improve-stories-2.0.xgb.gz";

    @Test
    public void testWithUrl() throws Exception {
        // This test requires environment variable to work properly:
        // DOWNLOAD_CACHE = /tmp

        DecisionModel model = DefaultDecisionModel.initWithUrl(MODEL_URL);

        List<Object> variants = DecisionTestWithModel.loadVariants("/datasets/2bs_bible_verses_full.json");


        Map<String, Object> context = DecisionTestWithModel.loadContext("/datasets/context_sample_1.json");

        Decision d = new DecisionMaker(variants, model, context);

        System.out.println(d.best());

    }

}
