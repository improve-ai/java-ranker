package ai.improve;

import static org.junit.Assert.fail;

import static ai.improve.TestModelValidation.getContext;
import static ai.improve.TestModelValidation.toMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

@RunWith(AndroidJUnit4.class)
public class TestScorer {
    public static final String Tag = "TestScorer";

    public static final String ModelUrl = "https://improveai-mindblown-mindful-prod-models.s3.amazonaws.com/models/latest/songs-2.0.xgb.gz";

    public static final String DummyV7ModelUrl = "file:///android_asset/dummy_v7.xgb";

    public static final String DummyV8ModelUrl = "file:///android_asset/dummy_v8.xgb";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testScore() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        List scores = scorer.score(Arrays.asList(0, 1, 2));
        IMPLog.d(Tag, "scores: " + scores);
    }

    @Test
    public void testScore_null_items() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        try {
            scorer.score(null);
            fail("items can't be null");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
    }

    @Test
    public void testScore_empty_items() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        try {
            scorer.score(new ArrayList<>());
            fail("items can't be empty");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
    }

    @Test
    public void testScore_with_context() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        List scores = scorer.score(Arrays.asList(0, 1, 2), "context");
        IMPLog.d(Tag, "scores: " + scores);
    }

    @Test
    public void testScore_complex_objects() throws JSONException, IOException, InterruptedException {
        JSONObject root = TestUtils.loadJson(getContext(), "complex.json");
        Map item = toMap(root);
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        List scores = scorer.score(Arrays.asList(item));
        IMPLog.d(Tag, "scores: " + scores);
    }

    @Test
    public void testScore_non_json_encodable() throws JSONException, IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        try {
            scorer.score(Arrays.asList(new Date()));
            fail("items must be JSON encodable");
        } catch (RuntimeException e) {
            return;
        }
    }

    @Test
    public void testLoad_v8_model() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        List scores = scorer.score(Arrays.asList(0, 1, 2));
        IMPLog.d(Tag, "scores: " + scores);
    }

    @Test
    public void testLoad_v7_model() throws InterruptedException {
        try {
            new Scorer(new URL(DummyV7ModelUrl));
            fail("v8 sdk should not load v7 models!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
