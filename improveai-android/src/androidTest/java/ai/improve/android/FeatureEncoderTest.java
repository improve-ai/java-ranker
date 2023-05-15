package ai.improve.android;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.AssertionFailedError;
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
        List<Object> items;

        if(testCase.isNull("item")) {
            items = new ArrayList<>();
            items.add(null);
        } else {
            Object item = root.getJSONObject("test_case").get("item");
            if (item instanceof JSONObject) {
                items = Arrays.asList(toMap((JSONObject) item));
            } else if (item instanceof JSONArray) {
                items = Arrays.asList(toList((JSONArray) item));
            } else {
                items = Arrays.asList(item);
            }
        }

        Object context = null;
        if (root.getJSONObject("test_case").has("context")) {
            Object contextObject = root.getJSONObject("test_case").get("context");
            if(contextObject instanceof JSONObject) {
                context = (Object) contextObject;
            }
        }

        HashMap<String, List<Long>> stringTables = getStringTablesFromTestCaseJSON(root);
        long modelSeed = root.getLong("model_seed");
        double noise = root.getDouble("noise");
        List<String> featureNames = getFeatureNames(root);

        double[] expected = getExpectedEncodingsListFromJSON(root);

        // extract feature names from test case JSON
        FeatureEncoder featureEncoder = new FeatureEncoder(featureNames, stringTables, modelSeed);
//        featureEncoder.noise = noise;

        List<FVec> encodedFeaturesFVecs = featureEncoder.encodeItemsForPrediction(items, context, noise);

        assertEquals(encodedFeaturesFVecs.size(), 1);
        return isEqual(expected, encodedFeaturesFVecs.get(0));
    }

    List<String> getAllKeysOfJSONObject(JSONObject object) {
        List<String> result = new ArrayList<>();
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            result.add(keys.next());
        }
        return result;
    }

    public List<String> getFeatureNames(JSONObject testCaseRoot) throws org.json.JSONException {
        List<String> featureNames = new ArrayList<>();
        JSONArray featureNamesJSON = testCaseRoot.getJSONArray("feature_names");
        for (int i = 0; i < featureNamesJSON.length(); ++i) {
            featureNames.add(featureNamesJSON.getString(i));
        }
        return featureNames;
    }

    public HashMap<String, List<Long>> getStringTablesFromTestCaseJSON(JSONObject testCaseRoot) throws org.json.JSONException {

        JSONObject stringTablesJSON = testCaseRoot.getJSONObject("string_tables");

        Iterator<String> stringFeatures = stringTablesJSON.keys();

        // check if string tables are an empty map -> if so return
        if (!stringFeatures.hasNext()) {
            return new HashMap<String, List<Long>>();
        }

        HashMap<String, List<Long>> stringTables = new HashMap<>();

        while (stringFeatures.hasNext()) {
            String stringFeatureName = stringFeatures.next();
            // unpack list of longs from JSON into JSON Array
            JSONArray hashedFeatureValuesJSON = stringTablesJSON.getJSONArray(stringFeatureName);
            // prepare container for the extracted masked string Hashes
            List<Long> stringFeatureHashes = new ArrayList<>();
            for (int i = 0; i < hashedFeatureValuesJSON.length(); ++i) {
                // get long from each element of JSON array
                stringFeatureHashes.add(hashedFeatureValuesJSON.getLong(i));
            }
            // update stringTables with the extracted masked string Hashes
            stringTables.put(stringFeatureName, stringFeatureHashes);

        }

        return stringTables;
    }

    public double[] getExpectedEncodingsListFromJSON(JSONObject testCaseRoot) throws org.json.JSONException {
        JSONArray expectedJSON = testCaseRoot.getJSONArray("test_output");
//        List<Double> expected = new ArrayList<>();
        double[] expected = new double[expectedJSON.length()];
        Arrays.fill(expected, Double.NaN);
        for (int i = 0; i < expectedJSON.length(); ++i) {
            expected[i] = expectedJSON.getDouble(i);
        }
        return expected;
    }

    boolean isEqual(double[] expected, FVec testOutput) {

        double encodedValue, expectedValue;

        for (int i = 0; i < expected.length; i++) {
            // this will not raise for an index out of bounds
            encodedValue = testOutput.fvalue(i);
            expectedValue = expected[i];

            if (expectedValue != encodedValue) {
                return false;
            }

        }

        return true;

//        // Make sure that all feature names in the testsuite can be found in feature names list.
//        // Just to be sure that we have not missed any feature name
//        for(int i = 0; i < expected.size(); ++i) {
//            String key = allKeysInExpected.get(i);
//            boolean found = false;
//            for(int j = 0; j < featureNames.size(); ++j) {
//                if(featureNames.get(j).equals(key)) {
//                    found = true;
//                    break;
//                }
//            }
//
//            assertTrue(found);
//        }
//
//
//        for(int i = 0; i < featureNames.size(); ++i) {
//            String featureName = featureNames.get(i);
//
//            // each element in expected must match the corresponding element in testOutput
//
//            if(expected.has(featureName)) {
//                Object valueObject = expected.get(featureName);
//                if(valueObject instanceof String) {
//                    // infinity is the only exception
//                    assertEquals("inf", valueObject);
//                    assertTrue(Float.isInfinite(testOutput.fvalue(i)));
//                } else {
//                    assertEquals((float)expected.getDouble(featureName), testOutput.fvalue(i), 0.0000001);
//                }
//            } else {
//                assertTrue(Float.isNaN(testOutput.fvalue(i)));
//            }
//        }
//        return true;
    }

    @Test
    public void testNAN() throws JSONException {
        List<String> featureNames = new ArrayList<String>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        HashMap<String, List<Long>> stringTables = new HashMap<String, List<Long>>();
        long modelSeed = 0;

        FeatureEncoder featureEncoder = new FeatureEncoder(featureNames, stringTables, modelSeed);
        double noise = 0.8928601514360016;
//        featureEncoder.noise = 0.8928601514360016;

        Object item = Double.NaN;

        // TODO do we need noise as a class attribute ?
        List<FVec> features = featureEncoder.encodeItemsForPrediction(new ArrayList<>(Arrays.asList(item)), null, noise);
        assertEquals(features.size(), 1);

        int oobIndex = 10000;
        System.out.println("Get index out of bounds from FVec: " +
                features.get(0).fvalue(oobIndex));

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
        InputStream inputStream = appContext.getAssets().open("feature_encoder_test_suite/multiple_variants.json");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        JSONObject root = new JSONObject(content);
        JSONObject testCase = root.getJSONObject("test_case");

        List<Object> items = toList(testCase.getJSONArray("items"));

        Object givensObject = testCase.get("context");
        Map<String, Object> context = toMap((JSONObject) givensObject);

        HashMap<String, List<Long>> stringTables = getStringTablesFromTestCaseJSON(root);
        long modelSeed = root.getLong("model_seed");
        double noise = root.getDouble("noise");
        List<String> featureNames = getFeatureNames(root);

//        JSONArray expected = root.getJSONArray("test_output");
        double[] expected = getExpectedEncodingsListFromJSON(root);

        FeatureEncoder featureEncoder = new FeatureEncoder(featureNames, stringTables, modelSeed);
        // TODO do we need noise as a class attribute ?
//        featureEncoder.noise = noise;
        List<FVec> features = featureEncoder.encodeItemsForPrediction(items, context, noise);
        assertEquals(2, features.size());

//        assertTrue(isEqual(expected.getJSONObject(0), features.get(0)));
//        assertTrue(isEqual(expected.getJSONObject(1), features.get(1)));

        assertTrue(isEqual(expected, features.get(0)));
        assertTrue(isEqual(expected, features.get(1)));
    }

//    @Test
//    public void testHashToFeatureName() {
//        FeatureEncoder featureEncoder = new FeatureEncoder(0, featureNames);
//        featureEncoder.hash_to_feature_name(0);
//        assertEquals(String.format("%08x", 0xffffffff), featureEncoder.hash_to_feature_name(0xffffffffffffffffL));
//        assertEquals(String.format("%08x", 0xfffffffe), featureEncoder.hash_to_feature_name(0xfffffffefffffffeL));
//        assertEquals(String.format("%08x", 0x8fffffff), featureEncoder.hash_to_feature_name(0x8fffffffffffffffL));
//        assertEquals(String.format("%08x", 0x7fffffff), featureEncoder.hash_to_feature_name(0x7fffffffffffffffL));
//        assertEquals(String.format("%08x", 0xfffffff), featureEncoder.hash_to_feature_name(0xfffffffffffffffL));
//    }
}