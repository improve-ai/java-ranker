package ai.improve.android;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;
import ai.improve.encoder.FeatureEncoder;
import biz.k11i.xgboost.util.FVec;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class FeatureEncoderTest {
    public static final String Tag = "FeatureEncoderTest";

    private List<String> featureNames;

    @Before
    public void setUp() throws Exception {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        loadFeatureNames();
    }

    /**
     * How is the feature names collected?
     *
     * 1. Run testFeatureEncoder for once, and let all test cases pass
     * 2. Filter the logcat output with tag "FeatureEncoder"
     * 3. Copy all the feature names to 'txt'
     * 4. cat txt | awk -F " " '{print $6}' | sort | uniq > feature_names.txt
     * 5. cp feature_names.txt ~/workspace/improve-android/improveai-android/src/androidTest/assets/
     * */
    private void loadFeatureNames() throws IOException {
        String rootDir = "feature_encoder_test_suite/";
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream inputStream = appContext.getAssets().open(rootDir + "feature_names.txt");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        String[] allFeatureNames = content.split("\\n");

        featureNames = new ArrayList<>(Arrays.asList(allFeatureNames));
    }

    @Test
    public void testFeatureEncoder() throws Exception {
        String rootDir = "feature_encoder_test_suite/";
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream inputStream = appContext.getAssets().open(rootDir + "feature_encoder_test_suite.txt");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        String[] allTestCases = content.split("\\n");
        for (int i = 0; i < allTestCases.length; ++i) {
            IMPLog.d(Tag, i + ", verify case " + allTestCases[i]);
            assertTrue(verify(rootDir + allTestCases[i]));
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
        JSONObject testCase = root.getJSONObject("test_case");
        List variants;

        if(testCase.isNull("variant")) {
            variants = new ArrayList<>();
            variants.add(null);
        } else {
            Object variant = root.getJSONObject("test_case").get("variant");
            if (variant instanceof JSONObject) {
                variants = Arrays.asList(toMap((JSONObject) variant));
            } else if (variant instanceof JSONArray) {
                variants = Arrays.asList(toList((JSONArray) variant));
            } else {
                variants = Arrays.asList(variant);
            }
        }

        Map givens = null;
        if (root.getJSONObject("test_case").has("givens")) {
            Object givensObject = root.getJSONObject("test_case").get("givens");
            if(givensObject instanceof JSONObject) {
                givens = toMap((JSONObject) givensObject);
            }
        }

        long modelSeed = root.getLong("model_seed");
        double noise = root.getDouble("noise");
        JSONObject expected = root.getJSONObject("test_output");

        FeatureEncoder featureEncoder = new FeatureEncoder(modelSeed, featureNames);
        featureEncoder.noise = noise;
        List<FVec> features = featureEncoder.encodeVariants(variants, givens);
        assertEquals(features.size(), 1);
        return isEqual(expected, features.get(0));
    }

    List<String> getAllKeysOfJSONObject(JSONObject object) {
        List<String> result = new ArrayList<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            result.add(keys.next());
        }
        return result;
    }

    boolean isEqual(JSONObject expected, FVec testOutput) throws JSONException {
        List<String> allKeysInExpected = getAllKeysOfJSONObject(expected);
        assertTrue(allKeysInExpected.size() >= 0);

        // Make sure that all feature names in the testsuite can be found in feature names list.
        // Just to be sure that we have not missed any feature name
        for(int i = 0; i < allKeysInExpected.size(); ++i) {
            String key = allKeysInExpected.get(i);
            boolean found = false;
            for(int j = 0; j < featureNames.size(); ++j) {
                if(featureNames.get(j).equals(key)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        for(int i = 0; i < featureNames.size(); ++i) {
            String featureName = featureNames.get(i);
            if(expected.has(featureName)) {
                Object valueObject = expected.get(featureName);
                if(valueObject instanceof String) {
                    // infinity is the only exception
                    assertEquals("inf", valueObject);
                    assertTrue(Float.isInfinite(testOutput.fvalue(i)));
                } else {
                    assertEquals((float)expected.getDouble(featureName), testOutput.fvalue(i), 0.0000001);
                }
            } else {
                assertTrue(Float.isNaN(testOutput.fvalue(i)));
            }
        }
        return true;
    }

    @Test
    public void testNAN() throws JSONException {
        FeatureEncoder featureEncoder = new FeatureEncoder(1, featureNames);
        featureEncoder.noise = 0.8928601514360016;

        Object variant = Double.NaN;

        List<FVec> features = featureEncoder.encodeVariants(new ArrayList<>(Arrays.asList(variant)), null);
        assertEquals(features.size(), 1);

        for(int i = 0; i < featureNames.size(); ++i) {
            assertTrue(Float.isNaN(features.get(0).fvalue(i)));
        }
    }

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

    public static Map<String, Object> toMap(JSONObject jsonobj)  throws JSONException {
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator<String> keys = jsonobj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            Object value = jsonobj.get(key);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }   return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }
            else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }   return list;
    }

    @Test
    public void testEncodeMultipleVariants() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InputStream inputStream = appContext.getAssets().open("multiple_variants.json");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        JSONObject root = new JSONObject(content);
        JSONObject testCase = root.getJSONObject("test_case");

        List variants = toList(testCase.getJSONArray("variants"));

        Object givensObject = testCase.get("givens");
        Map givens = toMap((JSONObject) givensObject);

        long modelSeed = root.getLong("model_seed");
        double noise = root.getDouble("noise");
        JSONArray expected = root.getJSONArray("test_output");


        FeatureEncoder featureEncoder = new FeatureEncoder(modelSeed, featureNames);
        featureEncoder.noise = noise;
        List<FVec> features = featureEncoder.encodeVariants(variants, givens);
        assertEquals(2, features.size());

        assertTrue(isEqual(expected.getJSONObject(0), features.get(0)));
        assertTrue(isEqual(expected.getJSONObject(1), features.get(1)));
    }
}