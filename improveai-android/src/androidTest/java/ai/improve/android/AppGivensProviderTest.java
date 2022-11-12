package ai.improve.android;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ai.improve.Decision;
import ai.improve.DecisionModel;
import ai.improve.log.IMPLog;

import static org.junit.Assert.*;
import static ai.improve.DecisionTrackerTest.Track_URL;

@RunWith(AndroidJUnit4.class)
public class AppGivensProviderTest {
    public static final String Tag = "AppGivensProviderTest";

    private Context context;

    @Before
    public void setUp() {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DecisionModel.setDefaultTrackURL(Track_URL);
    }

    @Test
    public void testOverlappingGivensKey() {
        Map<String, Object> userGivens = new HashMap();
        userGivens.put(AppGivensProvider.APP_Givens_Key_Language, "hi");

        DecisionModel decisionModel = new DecisionModel("hello");
        AppGivensProvider appGivensProvider = new AppGivensProvider(context);

        Map allGivens = appGivensProvider.givensForModel(decisionModel, null);
        IMPLog.d(Tag, "allGivens: " + allGivens);

        // assert that APP_Given_Key_Language exists in AppGivensProvider givens
        assertNotNull(allGivens.get(AppGivensProvider.APP_Givens_Key_Language));
        assertNotEquals("hi", allGivens.get(AppGivensProvider.APP_Givens_Key_Language));

        allGivens = appGivensProvider.givensForModel(decisionModel, userGivens);
        IMPLog.d(Tag, "allGivens: " + allGivens);

        // assert that user givens wins in case of overlapping
        assertEquals("hi", allGivens.get(AppGivensProvider.APP_Givens_Key_Language));
    }

    @Test
    public void testNullGivens() {
        Map<String, Object> userGivens = null;
        Map combinedGivens = new AppGivensProvider(context).givensForModel(new DecisionModel("hello"), userGivens);
        assertNotNull(combinedGivens);
        assertTrue(combinedGivens.size() > 0);
        IMPLog.d(Tag, "givens" + combinedGivens);
    }

    @Test
    public void testAddReward_decision() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        Decision decision = decisionModel.decide(Arrays.asList(1, 2, 3));
        decision.track();
        double oldTotalRewardsOfModel = AppGivensProviderUtils.rewardOfModel(context, modelName);
        decision.addReward(0.1);
        double newTotalRewardsOfModel = AppGivensProviderUtils.rewardOfModel(context, modelName);
        assertEquals(oldTotalRewardsOfModel+0.1, newTotalRewardsOfModel, 0.0000000001);
        IMPLog.d(Tag, "old reward: " + oldTotalRewardsOfModel +
                ", new reward: " + newTotalRewardsOfModel);
    }

    @Test
    public void testAddReward_decisionModel() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        double oldTotalRewardsOfModel = AppGivensProviderUtils.rewardOfModel(context, modelName);
        decisionModel.addReward(0.1);
        double newTotalRewardsOfModel = AppGivensProviderUtils.rewardOfModel(context, modelName);
        assertEquals(oldTotalRewardsOfModel+0.1, newTotalRewardsOfModel, 0.0000000001);
        IMPLog.d(Tag, "old reward: " + oldTotalRewardsOfModel +
                ", new reward: " + newTotalRewardsOfModel);
    }

    @Test
    public void testAddRewardForDecision() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        double oldTotalRewards = AppGivensProviderUtils.rewardOfModel(context, modelName);
        decisionModel.addReward(0.1, "decision_id");
        double newTotalRewards = AppGivensProviderUtils.rewardOfModel(context, modelName);
        assertEquals(oldTotalRewards+0.1, newTotalRewards, 0.000000000001);
        IMPLog.d(Tag, "old reward: " + oldTotalRewards +
                ", new reward: " + newTotalRewards);
    }

    @Test
    public void testVersion_string_length() throws JSONException {
        double v = AppGivensProviderUtils.versionToNumber("7.1.2");
        JSONObject root = new JSONObject();
        root.put("v", v);
        assertEquals("{\"v\":7.001002}", root.toString());
    }
}
