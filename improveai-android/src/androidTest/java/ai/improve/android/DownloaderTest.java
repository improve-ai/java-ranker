package ai.improve.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;

import ai.improve.DecisionModel;

@RunWith(AndroidJUnit4.class)
public class DownloaderTest {

    @Test
    public void testLoad_asset() throws IOException {
        URL modelUrl = new URL("file:///android_asset/dummy_v6.xgb");
        DecisionModel decisionModel = new DecisionModel("dummy");
        decisionModel.load(modelUrl);
    }

    @Test
    public void testLoad_asset_gzip() throws IOException {
        URL modelUrl = new URL("file:///android_asset/dummy_v5.xgb");
        DecisionModel decisionModel = new DecisionModel("dummy");
        decisionModel.load(modelUrl);
    }
}
