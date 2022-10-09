package ai.improve;

import android.content.Context;

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

import ai.improve.android.AppGivensProvider;
import ai.improve.log.IMPLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static java.lang.Math.pow;

@RunWith(AndroidJUnit4.class)
public class DecisionModelTest {
    public static final String Tag = "DecisionModelTest";

    public static final String ModelURL = "https://improveai-mindblown-mindful-prod-models.s3.amazonaws.com/models/latest/songs-2.0.xgb.gz";

    private static final String CompressedModelURL = "https://improveai-mindblown-mindful-prod-models.s3.amazonaws.com/models/latest/songs-2.0.xgb.gz";

    private static final String AssetModelFileName = "dummy_v6.xgb";

    public static final String DefaultFailMessage = "A runtime exception should have been thrown, we should never have reached here";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
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

    public DecisionModel model() {
        return new DecisionModel("greetings");
    }

    public DecisionModel loadedModel() throws Exception {
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.load(new URL(ModelURL));
        return decisionModel;
    }

    private List<String> variants() {
        return Arrays.asList("Hello", "Hi", "Hey");
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
    public void testGivensProvider() {
        assertTrue(model().getGivensProvider() instanceof AppGivensProvider);
        DecisionModel.setDefaultGivensProvider(null);
        assertNull(model().getGivensProvider());
    }

    @Test
    public void testDefaultGivensProvider() {
        assertTrue(DecisionModel.getDefaultGivensProvider() instanceof AppGivensProvider);
    }

    @Test
    public void testGet() throws Exception {
        List<String> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        URL url = new URL(ModelURL);
        String greeting = getDecisionModel("hello").load(url).decide(variants).get();
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
    public void testLoadAsync_no_callback() throws MalformedURLException {
        URL url = new URL(ModelURL);
        DecisionModel.get("greetings").loadAsync(url);
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
                fail("onError should not be called while loading an valid model");
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testLoadAsync_invalid_model_file() throws MalformedURLException, InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        URL url = new URL("file:///android_asset/dummy_outdated.xgb");
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.loadAsync(url, new DecisionModel.LoadListener() {
            @Override
            public void onLoad(DecisionModel model) {
                fail("onLoad should not be called for an invalid model");
                semaphore.release();
            }

            @Override
            public void onError(IOException e) {
                assertNotNull(e);
                semaphore.release();
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testLoadAsync_url_not_exist() throws MalformedURLException, InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        URL url = new URL("http://127.0.0.1/not/exist/model.xgb");
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.loadAsync(url, new DecisionModel.LoadListener() {
            @Override
            public void onLoad(DecisionModel model) {
                fail("onLoad should not be called");
                semaphore.release();
            }

            @Override
            public void onError(IOException e) {
                semaphore.release();
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testLoadAsync_null_listener() throws MalformedURLException, InterruptedException {
        URL modelURL = new URL("file:///android_asset/dummy_v6.xgb");
        DecisionModel decisionModel = new DecisionModel("greetings");
        decisionModel.loadAsync(modelURL, null);
        Thread.sleep(3 * 1000);
        assertTrue(decisionModel.isLoaded());
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
    public void testIsLoaded() throws Exception {
        DecisionModel decisionModel = new DecisionModel("greetings");
        assertFalse(decisionModel.isLoaded());
        decisionModel.load(new URL(ModelURL));
        assertTrue(decisionModel.isLoaded());
    }

    @Test
    public void testChooseFrom() throws Exception {
        URL modelUrl = new URL(ModelURL);
        Map<String, Object> given = new HashMap<>();
        given.put("language", "cowboy");
        // Choose from string
        getDecisionModel("hello").load(modelUrl).chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).get();
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
                    scores = decisionModel.given(toMap(givens.getJSONObject(i))).score(toList(variants));
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
        decisionModel.chooseFrom(Arrays.asList("Hello World", "Howdy World", "Hi World")).get();
        decisionModel.addReward(1/3.0);
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

    @Test
    public void testScore_null_variants() {
        DecisionModel decisionModel = getDecisionModel("hello");
        try {
            decisionModel.score(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testScore_empty_variants() {
        DecisionModel decisionModel = getDecisionModel("hello");
        try {
            decisionModel.score(new ArrayList<>());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testScore_valid() {
        DecisionModel decisionModel = getDecisionModel("hello");
        decisionModel.score(Arrays.asList(1, 2, 3));
    }

    @Test
    public void testScore_consistent_encoding() throws IOException {
        int loop = 10;
        for(int i = 0; i < loop; ++i) {
            String path = "1000_list_of_numeric_variants_20_same_nested_givens_binary_reward";
            URL modelUrl = new URL("file:///android_asset/validate_models/" + path + "/model.xgb");
            DecisionModel decisionModel = getDecisionModel("hello").load(modelUrl);
            assertNotNull(decisionModel);

            List variants = Arrays.asList(1.0, 2);
            Map child = new HashMap() {{
                put("d", Arrays.asList(0.0, 1.2, 2));
                put("e", true);
                put("f", "AsD");
            }};
            Map givens = new HashMap() {{
                put("a", "b");
                put("c", child);
            }};

            // first call of model.score()
            List<Double> scores_1 = decisionModel.given(givens).score(Arrays.asList(variants, variants));
            assertEquals(2, scores_1.size());
            assertEquals(scores_1.get(0), scores_1.get(1), 0.000001);
            IMPLog.d(Tag, "score 1: " + scores_1 + ", diff=" + (scores_1.get(0) - scores_1.get(1)));

            // second call of model.score()
            List<Double> scores_2 = decisionModel.given(givens).score(Arrays.asList(variants, variants));
            assertEquals(2, scores_2.size());
            assertEquals(scores_2.get(0), scores_2.get(1), 0.000001);
            IMPLog.d(Tag, "score 2: " + scores_1 + ", diff=" + (scores_2.get(0) - scores_2.get(1)));

            // Scores of the first and second call should differ because of the random noise
            // in the FeatureEncoder. However, if the noises happens to be very close to each
            // other, the scores can be very similar as well, and the following assertion might
            // fail.
            assertNotEquals(scores_1.get(0), scores_2.get(0), 0.000001);
            IMPLog.d(Tag, "score diff: " + (scores_1.get(0) - scores_2.get(0)));
        }
    }

    @Test
    public void testA_2_Z_Model() throws IOException, JSONException {
        for(int k = 0; k < 100; ++k) {
            URL modelUrl = new URL("file:///android_asset/a_z_model/model.xgb");
            DecisionModel decisionModel = new DecisionModel("a-z");
            decisionModel.load(modelUrl);

            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            InputStream inputStream = appContext.getAssets().open("a_z_model/a_z.json");
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String content = new String(buffer);
            JSONObject root = new JSONObject(content);
            JSONObject testCase = root.getJSONObject("test_case");

            double noise = testCase.getDouble("noise");
            decisionModel.getFeatureEncoder().noise = noise;
            IMPLog.d(Tag, "noise: " + noise);

            List variants = toList(testCase.getJSONArray("variants"));
            IMPLog.d(Tag, "variants: " + variants);
            assertEquals(26, variants.size());

            List expectedScores = toList(root.getJSONArray("expected_output").getJSONObject(0).getJSONArray("scores"));
            assertEquals(26, expectedScores.size());

            List<Double> scores = decisionModel.score(variants);
            IMPLog.d(Tag, "scores: " + scores);

            for (int i = 0; i < 26; ++i) {
                assertEquals((double) expectedScores.get(i), scores.get(i), 0.000002);
            }
        }
    }

    @Test
    public void testArrayEncodingWarning() throws IOException {
        List variants = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(4, 5, 6));
        URL modelUrl = new URL("file:///android_asset/a_z_model/model.xgb");
        DecisionModel decisionModel = new DecisionModel("a-z");
        decisionModel.load(modelUrl);
        decisionModel.chooseFrom(variants).get();
        decisionModel.chooseFrom(variants).get();
    }

    // Tests that decision is tracked when calling which().
    @Test
    public void testWhich_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.which(1, 2, 3);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown.
    @Test
    public void testWhich_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.which(1, 2, 3);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is tracked when calling whichFrom().
    @Test
    public void testWhichFrom_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.whichFrom(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown.
    @Test
    public void testWhichFrom_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.whichFrom(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked when calling rank().
    @Test
    public void testRank_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.rank(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown
    @Test
    public void testRank_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.rank(Arrays.asList(1, 2, 3));
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is tracked when calling optimize().
    @Test
    public void testOptimize_track() {
        Map<String, List> variantMap = new HashMap<>();
        variantMap.put("font", Arrays.asList(12, 13));
        variantMap.put("color", Arrays.asList("white", "black"));
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.optimize(variantMap);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown
    @Test
    public void testOptimize_null_trackURL() {
        Map<String, List> variantMap = new HashMap<>();
        variantMap.put("font", Arrays.asList(12, 13));
        variantMap.put("color", Arrays.asList("white", "black"));
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.optimize(variantMap);
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is tracked when calling optimize().
    @Test
    public void testFirst_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.first("hi", "hello", "hey");
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown
    @Test
    public void testFirst_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.first("hi", "hello", "hey");
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is tracked when calling optimize().
    @Test
    public void testRandom_track() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.random("hi", "hello", "hi");
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertNotEquals(lastDecisionId, newDecisionId);
    }

    // Tests that decision is not tracked and no exceptions thrown
    @Test
    public void testRandom_null_trackURL() {
        String modelName = "greetings";
        DecisionModel decisionModel = new DecisionModel(modelName);
        decisionModel.setTrackURL(null);
        String lastDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        decisionModel.random("hi", "hello", "hi");
        String newDecisionId = DecisionTracker.persistenceProvider.lastDecisionIdForModel(modelName);
        IMPLog.d(Tag, "decisionId: " + lastDecisionId + ", " + newDecisionId);
        assertNotNull(newDecisionId);
        assertEquals(lastDecisionId, newDecisionId);
    }

    @Test
    public void testDecide_ordered_true_not_loaded() {
        List<String> variants = variants();
        List<String> rankedVariants = model().decide(variants, true).ranked;
        assertTrue(variants != rankedVariants); // different object
        assertEquals(variants, rankedVariants);
    }

    @Test
    public void testDecide_ordered_true_loaded() throws Exception {
        List<String> variants = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        List<String> rankedVariants = loadedModel().decide(variants, true).ranked;
        assertTrue(variants != rankedVariants); // different object
        assertEquals(variants, rankedVariants);
    }

    @Test
    public void testDecide_ordered_false_not_loaded() {
        List<String> variants = variants();
        List<String> rankedVariants = model().decide(variants, false).ranked;
        assertTrue(variants != rankedVariants); // different object
        assertEquals(variants, rankedVariants);
    }

    @Test
    public void testDecide_ordered_false_loaded() throws Exception {
        List<String> variants = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        List<String> rankedVariants = loadedModel().decide(variants, false).ranked;
        assertTrue(variants != rankedVariants); // different object
        assertNotEquals(variants, rankedVariants);
    }

    @Test
    public void testTrack() throws InterruptedException {
        String variant = "hi";
        List<String> runnersUp = Arrays.asList("hello", "hey");
        String sample = "ha";
        int samplePoolSize = 4;
        String decisionId = model().track(variant, runnersUp, sample, samplePoolSize);
        assertNotNull(decisionId);
        Thread.sleep(3000);
    }
}
