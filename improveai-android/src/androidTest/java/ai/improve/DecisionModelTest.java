package ai.improve;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import ai.improve.log.IMPLog;

import static ai.improve.DecisionTrackerTest.Track_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static java.lang.Math.pow;

@RunWith(AndroidJUnit4.class)
public class DecisionModelTest {
    public static final String Tag = "DecisionModelTest";

    public static final String ModelURL = "https://improveai-mindblown-mindful-prod-models.s3.amazonaws.com/models/latest/improveai-songs-2.0.xgb.gz";

    private static final String CompressedModelURL = "https://improveai-mindblown-mindful-prod-models.s3.amazonaws.com/models/latest/improveai-songs-2.0.xgb.gz";

    private static final String AssetModelFileName = "dummy_v6.xgb";

    public static final String DefaultFailMessage = "A runtime exception should have been thrown, we should never have reached here";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        DecisionModel.setDefaultTrackURL(Track_URL);
    }

//
//    @Test
//    public void testLoadFromAsset() throws Exception {
//        IMPLog.d(Tag, "testLoadFromAsset...");
//        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        DecisionModel decisionModel = DecisionModel.loadFromAsset(appContext, "dummy_v6.xgb");
//        assertNotNull(decisionModel);
//    }
//
//    @Test
//    public void testLoadFromAssetAsync() throws InterruptedException {
//        Semaphore semaphore = new Semaphore(0);
//        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        DecisionModel decisionModel = new DecisionModel("music");
//        decisionModel.loadFromAssetAsync(appContext, "dummy_v6.xgb", new DecisionModel.IMPDecisionModelLoadListener() {
//            @Override
//            public void onFinish(DecisionModel model, Exception e) {
//                assertNotNull(model);
//                assertNull(e);
//                semaphore.release();
//            }
//        });
//        semaphore.acquire();
//    }

    public DecisionModel getDecisionModel(String modelName) {
        return new DecisionModel(modelName);
    }

    @Test
    public void testModelNameWithoutLoadingModel() {
        DecisionModel decisionModel = new DecisionModel("music");
        assertEquals("music", decisionModel.getModelName());
    }

    @Test
    public void testModelName() throws Exception {
        URL url = new URL(ModelURL);
        DecisionModel decisionModel = getDecisionModel("hello").load(url);
        assertEquals("hello", decisionModel.getModelName());
    }

    @Test
    public void testGet() throws Exception {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        URL url = new URL(ModelURL);
        String greeting = (String) getDecisionModel("hello").load(url).chooseFrom(variants).get();
        IMPLog.d(Tag, "testGet, greeting=" + greeting);
        assertNotNull(greeting);
    }

    @Test
    public void testGetWithoutLoadingModel() throws MalformedURLException {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        // Don't load model
        String greeting = (String) new DecisionModel("theme").chooseFrom(variants).get();
        IMPLog.d(Tag, "greeting=" + greeting);
        assertNotNull(greeting);
    }

    @Test
    public void testLoadAsync() throws MalformedURLException, InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        URL url = new URL(ModelURL);
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.loadAsync(url, new DecisionModel.LoadListener() {
            @Override
            public void onLoad(DecisionModel model) {
                assertNotNull(model);
                IMPLog.d(Tag, "testLoadAsync, OK");
                semaphore.release();
            }

            @Override
            public void onError(IOException e) {
                assertNotNull(e);
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testLoadGzipModel() throws Exception {
        URL url = new URL(CompressedModelURL);
        DecisionModel decisionModel = getDecisionModel("hello").load(url);
        assertNotNull(decisionModel);

        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        String greeting = (String) decisionModel.chooseFrom(variants).get();
        IMPLog.d(Tag, "testLoadGzipModel, greeting=" + greeting);
        assertNotNull(greeting);
    }

    @Test
    public void testLoadLocalModel() throws Exception {
        // Download model file and save it to external cache dir
        String localModelFilePath = download(ModelURL);
        URL url = new File(localModelFilePath).toURI().toURL();

        DecisionModel decisionModel = getDecisionModel("hello").load(url);
        assertNotNull(decisionModel);

        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        String greeting = (String) decisionModel.chooseFrom(variants).get();
        IMPLog.d(Tag, "testLoadLocalModel, greeting=" + greeting);
        assertNotNull(greeting);
    }

    @Test
    public void testLoadLocalCompressedModel() throws IOException {
        // Download model file and save it to external cache dir
        String localModelFilePath = download(CompressedModelURL);

        URL url = new File(localModelFilePath).toURI().toURL();

        DecisionModel decisionModel = getDecisionModel("hello").load(url);

        assertNotNull(decisionModel);

        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        String greeting = (String) decisionModel.chooseFrom(variants).get();
        IMPLog.d(Tag, "testLoadLocalCompressedModel, greeting=" + greeting);
        assertNotNull(greeting);
    }

    @Test
    public void testLoadNotExistModel() throws MalformedURLException {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        // Don't load model
        URL url = new URL(ModelURL + "/not/exist");
        String greeting = null;
        Exception loadException = null;
        try {
            greeting = (String) getDecisionModel("hello").load(url).chooseFrom(variants).get();
        } catch (Exception e) {
            loadException = e;
            e.printStackTrace();
        }
        assertNull(greeting);
        assertNotNull(loadException);
    }

    @Test
    public void testLoadFromNonMainThread() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        new Thread() {
            @Override
            public void run() {
                try {
                    List<Object> variants = new ArrayList<>();
                    variants.add("Hello, World!");
                    variants.add("hello, world!");
                    variants.add("hello");
                    variants.add("hi");

                    URL url = new URL(ModelURL);
                    String greeting = (String) getDecisionModel("hello").load(url).chooseFrom(variants).get();
                    IMPLog.d(Tag, "testGet, greeting=" + greeting);
                    assertNotNull(greeting);

                    semaphore.release();
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }.start();
        semaphore.acquire();
    }

    private String download(String urlStr) throws IOException, SecurityException {
        URL url = new URL(urlStr);
        InputStream is = new BufferedInputStream(url.openStream());
        byte[] buffer = new byte[1024];
        int length;

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String absfile = appContext.getExternalCacheDir() + "/" + UUID.randomUUID().toString() + ".xgb";
        if(urlStr.endsWith(".gz")) {
            absfile += ".gz";
        }
        IMPLog.d(Tag, "cache file path: " + absfile);

        // write to cache
        FileOutputStream fos = new FileOutputStream(new File(absfile));
        while ((length = is.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }

        return absfile;
    }

    @Test
    public void testChooseFrom() throws Exception {
        URL modelUrl = new URL(ModelURL);
        Map<String, Object> given = new HashMap<>();
        given.put("language", "cowboy");
        // Choose from string
        getDecisionModel("hello").load(modelUrl).chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).given(given).get();
    }

    @Test
    public void testChooseFromVariantsWithNull() throws Exception {
        Semaphore semaphore = new Semaphore(0);
        URL modelUrl = new URL(ModelURL);
        DecisionModel model = new DecisionModel("greetings");
        model.loadAsync(modelUrl, new DecisionModel.LoadListener() {
            @Override
            public void onLoad(DecisionModel model) {
                // the model is ready to go
                Object variant = model.chooseFrom(Arrays.asList(null, 0.1, 0.2)).get();
                IMPLog.d(Tag, "variant=" + variant);
                semaphore.release();
            }

            @Override
            public void onError(IOException e) {
                IMPLog.d(Tag, "Error loading model: " + e.getLocalizedMessage());
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testChooseFromNonJsonEncodable() throws MalformedURLException {
        List<Object> variants = new ArrayList<>();
        variants.add("hi");
        variants.add(new Date());

        URL url = new URL(ModelURL);
        try {
            getDecisionModel("hello").load(url).chooseFrom(variants).get();
        } catch (Exception e) {
            IMPLog.e(Tag, ""+e.getMessage());
            e.printStackTrace();
            return;
        }
        fail("A RuntimeException is expected. We should never reach here");
    }

    @Test
    public void testValidateModels() throws IOException, JSONException {
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

    private boolean verify(String path) throws IOException, JSONException {
        // Although the model files in the asset folder end with '.gz', they
        // are somehow stripped by Android aapt tool, so we have to remove
        // the '.gz' suffix to access it here.
        URL modelUrl = new URL("file:///android_asset/validate_models/" + path + "/model.xgb");
        DecisionModel decisionModel = getDecisionModel("hello").load(modelUrl);

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

        // set noise
        decisionModel.getFeatureEncoder().noise = noise;

        // disable tie-breaker
        decisionModel.enableTieBreaker = false;

        JSONArray variants = testCase.getJSONArray("variants");
        if(testCase.isNull("givens")) {
            List<Double> scores = decisionModel.score(toList(variants));
            JSONArray expectedScores = expectedOutputs.getJSONObject(0).getJSONArray("scores");
            assertEquals(scores.size(), expectedScores.length());
            assertTrue(scores.size() > 0);

            for(int i = 0; i < scores.size(); ++i) {
                assertEquals(expectedScores.getDouble(i), scores.get(i), pow(2, -20));
            }
        } else {
            JSONArray givens = testCase.getJSONArray("givens");
            for(int i = 0; i < givens.length(); ++i) {
                List<Double> scores;
                if(givens.isNull(i)) {
                    scores = decisionModel.score(toList(variants));
                } else {
                    scores = decisionModel.score(toList(variants), toMap(givens.getJSONObject(i)));
                }
                JSONArray expectedScores = expectedOutputs.getJSONObject(i).getJSONArray("scores");
                assertEquals(scores.size(), expectedScores.length());
                assertTrue(scores.size() > 0);

                for(int j = 0; j < scores.size(); ++j) {
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

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testAddReward_Android() {
        DecisionModel decisionModel = new DecisionModel("hello");
        decisionModel.addReward(0.1);
    }

    @Test
    public void testAddReward_Null_TrackURL() {
        try {
            // Just to verify that a warning message is printed
            DecisionModel decisionModel = new DecisionModel("hello", null, null);
            decisionModel.addReward(0.1);
        } catch (IllegalStateException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_NaN() {
        try {
            DecisionModel decisionModel = new DecisionModel("hello");
            decisionModel.addReward(Double.NaN);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_Positive_Infinity() {
        try {
            DecisionModel decisionModel = new DecisionModel("hello");
            decisionModel.addReward(Double.POSITIVE_INFINITY);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testAddReward_Negative_Infinity() {
        try {
            DecisionModel decisionModel = new DecisionModel("hello");
            decisionModel.addReward(Double.NEGATIVE_INFINITY);
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testLoadFromAsssets() throws IOException {
        DecisionModel decisionModel = new DecisionModel("hello");
        decisionModel.load(new URL("file:///android_asset/dummy_v6.xgb"));
    }
}
