package ai.improve;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import ai.improve.log.IMPLog;

@RunWith(AndroidJUnit4.class)
public class TestScorer {
    public static final String Tag = "TestScorer";

    public static final String ModelUrl = "https://improveai-mindblown-mindful-prod-models.s3.amazonaws.com/models/latest/songs-2.0.xgb.gz";

    public static final String DummyV8ModelUrl = "file:///android_asset/dummy_v8.xgb";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testScore() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(ModelUrl));
        List scores = scorer.score(Arrays.asList(0, 1, 2));
        IMPLog.d(Tag, "scores: " + scores);
    }

    @Test
    public void testLoad_v8_model() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        List scores = scorer.score(Arrays.asList(0, 1, 2));
        IMPLog.d(Tag, "scores: " + scores);
    }

}
