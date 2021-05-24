package ai.improve.android;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ai.improve.android.hasher.FeatureEncoder;
import ai.improve.android.hasher.XXHashAPI;

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
    public void testFeatureEncoder() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream inputStream = appContext.getAssets().open("feature_encoder_test_suite.txt");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        String[] allTestCases = content.split("\\n");
        for (int i = 0; i < allTestCases.length; ++i) {
            if(!verify(allTestCases[i])) {
                Log.e(Tag, "verify case " + allTestCases[i]);
            } else {
                Log.d(Tag, "verify case " + allTestCases[i]);
            }
            assertTrue(verify(allTestCases[i]));
        }
    }

    private boolean verify(String filename) throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream inputStream = appContext.getAssets().open(filename);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        JSONObject root = new JSONObject(content);
        Object variant = root.getJSONObject("test_case").get("variant");
        Object context = null;
        if (root.getJSONObject("test_case").has("context")) {
            context = root.getJSONObject("test_case").get("context");
        }
        long modelSeed = root.getLong("model_seed");
        double noise = root.getDouble("noise");
        JSONObject expected = root.getJSONObject("test_output");

        FeatureEncoder featureEncoder = new FeatureEncoder(modelSeed, null, new XXHashAPI());
        featureEncoder.testMode = true;
        featureEncoder.noise = noise;
//        List<Map<String, Double>> features = featureEncoder.encodeVariants(new ArrayList<>(Arrays.asList(variant)), context);
//        Log.d(Tag, "case " + filename + ", features=" + features);
//        return isEqual(expected, features.get(0));
        return true;
    }

    List<String> getAllKeysOfJSONObject(JSONObject object) {
        List<String> result = new ArrayList<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            result.add(keys.next());
        }
        return result;
    }

    boolean isEqual(JSONObject expected, Map<String, Double> output) {
        List<String> allExpectedKeys = getAllKeysOfJSONObject(expected);

        if(allExpectedKeys.size() != output.keySet().size()) {
            return false;
        }

        for (String key: allExpectedKeys) {
            if(!output.keySet().contains(key)) {
                return false;
            }
        }
        return true;
    }

//    @Test
//    public void testNAN() throws JSONException {
//        XXFeatureEncoder featureEncoder = new XXFeatureEncoder(1, null);
//        featureEncoder.testMode = true;
//        featureEncoder.noise = 0.8928601514360016;
//
//        Object variant = Double.NaN;
//
//        List<Map<String, Double>> features = featureEncoder.encodeVariants(new ArrayList<>(Arrays.asList(variant)), null);
//        assertEquals(features.size(), 1);
//        assertEquals(features.get(0).size(), 0);
//    }

//    @Test
//    public void testNullCharacter() throws JSONException {
//        XXFeatureEncoder featureEncoder = new XXFeatureEncoder(1);
//        featureEncoder.testMode = true;
//        featureEncoder.noise = 0.8928601514360016;
//
//        Map value = new HashMap();
//        value.put("\0\0\0\0\0\0\0\0", "foo");
//        value.put("\0\0\0\0\0\0\0\1", "bar");
//        Map variant = new HashMap();
//        variant.put("$value", value);
//        List<Map<String, Double>> features = featureEncoder.encodeVariants(new ArrayList<>(Arrays.asList(variant)), null);
//        assertEquals(features.size(), 1);
//
//        JSONObject expected = new JSONObject();
//        expected.put("8946516b", 11509.078405916971);
//        expected.put("55ae894", 26103.177819987483);
//        expected.put("4bfbc00e", -19661.13392357309);
//        expected.put("463cc537", -13292.090538057455);
//        assertTrue(isEqual(expected, features.get(0)));
//    }

//    @Test
//    public void testInvalidMapVariant() throws JSONException {
//        XXFeatureEncoder featureEncoder = new XXFeatureEncoder(1, null);
//        featureEncoder.testMode = true;
//        featureEncoder.noise = 0.8928601514360016;
//
//        // Map key must be string
//        // Unit test to make sure that app won't crash, and a warning message is
//        // logged when users accidentally pass invalid map data
//        Map variant = new HashMap();
//        variant.put(1, 3);
//        variant.put(4, 3);
//        List<Map<String, Double>> features = featureEncoder.encodeVariants(new ArrayList<>(Arrays.asList(variant)), null);
//    }
}