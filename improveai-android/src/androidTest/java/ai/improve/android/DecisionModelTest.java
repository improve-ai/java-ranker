package ai.improve.android;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import ai.improve.DecisionModel;
import ai.improve.DecisionTracker;
import ai.improve.log.IMPLog;

import static ai.improve.android.DecisionTrackerTest.Tracker_Url;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class DecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    public static final String ModelURL = "http://192.168.1.101/dummy_v6.xgb";

    private static final String CompressedModelURL = "http://192.168.1.101/dummy_v6.xgb.gz";

    private static final String AssetModelFileName = "dummy_v6.xgb";

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

    @Test
    public void testModelNameWithoutLoadingModel() {
        DecisionModel decisionModel = new DecisionModel("music");
        assertEquals("music", decisionModel.getModelName());
    }

    @Test
    public void testModelName() throws Exception {
        URL url = new URL(ModelURL);
        DecisionModel decisionModel = DecisionModel.load(url);
        IMPLog.d(Tag, "modelName=" + decisionModel.getModelName());
        assertEquals("dummy-model-0", decisionModel.getModelName());
    }

    @Test
    public void testGet() throws Exception {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        URL url = new URL(ModelURL);
        String greeting = (String) DecisionModel.load(url).chooseFrom(variants).get();
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
        String greeting = (String) new DecisionModel("").chooseFrom(variants).get();
        IMPLog.d(Tag, "greeting=" + greeting);
        assertNotNull(greeting);
    }

    @Test
    public void testLoadAsync() throws MalformedURLException, InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        URL url = new URL(ModelURL);
        DecisionModel decisionModel = new DecisionModel("music");
        decisionModel.loadAsync(url, new DecisionModel.IMPDecisionModelLoadListener() {
            @Override
            public void onFinish(DecisionModel model, Exception e) {
                assertNull(e);
                assertNotNull(model);
                IMPLog.d(Tag, "testLoadAsync, OK");
                semaphore.release();
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testLoadGzipModel() throws Exception {
        URL url = new URL(CompressedModelURL);
        DecisionModel decisionModel = DecisionModel.load(url);
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

        DecisionModel decisionModel = DecisionModel.load(url);
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
    public void testLoadLocalCompressedModel() throws Exception {
        // Download model file and save it to external cache dir
        String localModelFilePath = download(CompressedModelURL);

        URL url = new File(localModelFilePath).toURI().toURL();

        DecisionModel decisionModel = DecisionModel.load(url);
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
            greeting = (String) DecisionModel.load(url).chooseFrom(variants).get();
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
                    String greeting = (String) DecisionModel.load(url).chooseFrom(variants).get();
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
    public void testChooseFromAll() throws  Exception {
        URL modelUrl = new URL(ModelURL);
        Map<String, Object> given = new HashMap<>();
        given.put("language", "cowboy");

        // Choose from null
        DecisionModel.load(modelUrl).chooseFrom(null).get();


        // Choose from string
        DecisionModel.load(modelUrl).chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).given(given).get();

        // Choose from boolean
        DecisionModel.load(modelUrl).given(given).chooseFrom(Arrays.asList(true, false)).get();

        // loadFromAsset
//        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        DecisionModel.loadFromAsset(appContext, AssetModelFileName).chooseFrom(Arrays.asList("clutch", "dress", "jacket")).get();

        DecisionTracker tracker = new DecisionTracker(Tracker_Url);
        DecisionModel model = new DecisionModel("greetings");
        model.trackWith(tracker);
        model.loadAsync(modelUrl, new DecisionModel.IMPDecisionModelLoadListener() {
            @Override
            public void onFinish(DecisionModel model, Exception e) {
                if(e != null) {
                    Log.d(Tag, "Error loading model: " + e.getLocalizedMessage());
                } else {
                    // the model is ready to go
                    model.chooseFrom(Arrays.asList(0.1, 0.2, 0.3)).get();
                }
            }
        });
    }

    @Test
    public void testChooseFrom() throws Exception {
        URL modelUrl = new URL(ModelURL);
        Map<String, Object> given = new HashMap<>();
        given.put("language", "cowboy");
        // Choose from string
        DecisionModel.load(modelUrl).chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World")).given(given).get();
    }

    @Test
    public void testChooseFromVariantsWithNull() throws Exception {
        Semaphore semaphore = new Semaphore(0);
        URL modelUrl = new URL(ModelURL);
        DecisionTracker tracker = new DecisionTracker(Tracker_Url);
        DecisionModel model = new DecisionModel("greetings");
        model.trackWith(tracker);
        model.loadAsync(modelUrl, new DecisionModel.IMPDecisionModelLoadListener() {
            @Override
            public void onFinish(DecisionModel model, Exception e) {
                if(e != null) {
                    IMPLog.d(Tag, "Error loading model: " + e.getLocalizedMessage());
                    return ;
                }

                // the model is ready to go
                Object variant = model.chooseFrom(Arrays.asList(null, 0.1, 0.2)).get();
                IMPLog.d(Tag, "variant=" + variant);
                semaphore.release();
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testChooseFromNonJsonEncodable() throws MalformedURLException {
        List<Object> variants = new ArrayList<>();
        variants.add("hi");
        variants.add(new Date());

        URL url = new URL("https://kpz-1251356641.cos.ap-guangzhou.myqcloud.com/dummy_v6.xgb");
        try {
            DecisionModel.load(url).chooseFrom(variants).get();
        } catch (RuntimeException e) {
            IMPLog.e(Tag, ""+e.getMessage());
            e.printStackTrace();
            return;
        }
        fail("A RuntimeException is expected. We should never reach here");
    }
}
