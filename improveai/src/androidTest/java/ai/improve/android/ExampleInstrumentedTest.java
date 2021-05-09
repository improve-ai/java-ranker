package ai.improve.android;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

//import ai.improve.android.hasher.FeatureEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ai.improve.android.hasher.FeatureEncoder;
import ai.improve.android.hasher.XXFeatureEncoder;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    public static final String Tag = "ExampleInstrumentedTest";

    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Boolean x = Boolean.FALSE;
        if(x) {
            Log.d(Tag, "it is true");
        } else {
            Log.d(Tag, "it is false");
        }

    }

    @Test
    public void testFeatureEncoder() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream inputStream = appContext.getAssets().open("all_feature_encoder_test_cases.txt");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        String[] allTestCases = content.split("\\n");
        for (int i = 0; i < allTestCases.length; ++i) {
            Log.d(Tag, "verify case " + i + ": " + allTestCases[i]);
            verify(allTestCases[i]);
        }
    }

    private void verify(String filename) throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream inputStream = appContext.getAssets().open(filename);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        JSONObject root = new JSONObject(content);
        Object variant = root.getJSONObject("test_case").get("variant");

        long modelSeed = root.getLong("model_seed");
        double noise = root.getDouble("noise");

        XXFeatureEncoder featureEncoder = new XXFeatureEncoder(modelSeed);
        featureEncoder.testMode = true;
        featureEncoder.noise = noise;
        List<Map> features = featureEncoder.encodeVariants(new ArrayList<>(Arrays.asList(variant)), null);
        Log.d(Tag, "model_seed=" + modelSeed + ", features=" + features);
    }
}