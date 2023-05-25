package ai.improve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.lang.Math.pow;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

public class TestModelValidation {
    public static final String Tag = "TestModelValidation";

    public static Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testValidateModels() throws IOException, JSONException, InterruptedException {
        InputStream inputStream = getContext().getAssets().open("validate_models/model_test_suite.txt");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        String[] allTestCases = content.split("\\n");
        for (int i = 0; i < allTestCases.length; ++i) {
            IMPLog.d(Tag, i + ", verify case " + allTestCases[i]);
            assertTrue(verify(allTestCases[i]));
        }
    }

    private boolean verify(String path) throws IOException, JSONException, InterruptedException {
        // Although the model files in the asset folder end with '.gz', they
        // are somehow stripped by Android aapt tool, so we have to remove
        // the '.gz' suffix to access it here.
        URL modelUrl = new URL("file:///android_asset/validate_models/" + path + "/model.xgb");
        Scorer scorer = new Scorer(modelUrl);

        // load testcase json
        InputStream inputStream = getContext().getAssets().open("validate_models/" + path + "/" + path + ".json");
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        JSONObject root = new JSONObject(content);
        JSONObject testCase = root.getJSONObject("test_case");
        JSONArray expectedOutputs = root.getJSONArray("expected_output");
        double noise = testCase.getDouble("noise");
        IMPLog.d(Tag, path + ", noise = " + noise);

        JSONArray variants = testCase.getJSONArray("candidates");
        if(testCase.isNull("contexts")) {
            List<Double> scores = scorer.score(toList(variants), null, noise);
            JSONArray expectedScores = expectedOutputs.getJSONObject(0).getJSONArray("scores");
            assertEquals(scores.size(), expectedScores.length());
            assertTrue(scores.size() > 0);

            for(int i = 0; i < scores.size(); ++i) {
                assertEquals(expectedScores.getDouble(i), scores.get(i), pow(2, -20));
            }
        } else {
            JSONArray contexts = testCase.getJSONArray("contexts");
            for(int i = 0; i < contexts.length(); ++i) {
                List<Double> scores;
                if(contexts.isNull(i)) {
                    scores = scorer.score(toList(variants), null, noise);
                } else {
                    Object context = contexts.get(i);
                    if(context instanceof JSONObject) {
                        scores = scorer.score(toList(variants), toMap(contexts.getJSONObject(i)), noise);
                    } else {
                        scores = scorer.score(toList(variants), context, noise);
                    }
                }
                JSONArray expectedScores = expectedOutputs.getJSONObject(i).getJSONArray("scores");
                assertEquals(scores.size(), expectedScores.length());
                assertTrue(scores.size() > 0);

                for(int j = 0; j < scores.size(); ++j) {
                    IMPLog.d(Tag, "Scores, expected = " + expectedScores.getDouble(j) + ", real = " + scores.get(j));
                    assertEquals(expectedScores.getDouble(j), scores.get(j), pow(2, -18));
                }
            }
        }
        return true;
    }

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
            } else if(value == JSONObject.NULL) {
                value = null;
            }
            list.add(value);
        }   return list;
    }

}
