package ai.improve.android;

import android.content.Context;
import android.os.Looper;

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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import ai.improve.android.xgbpredictor.ImprovePredictor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class IMPDecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    private static final String ModelURL = "https://yamotek-1251356641.cos.ap-guangzhou.myqcloud.com/dummy_v6.xgb";

    private static final String CompressedModelURL = "https://yamotek-1251356641.cos.ap-guangzhou.myqcloud.com/dummy_v6.xgb.gz";

    static {
        IMPLog.setLogger(new IMPLoggerImp());
        IMPLog.enableLogging(true);
    }

    @Test
    public void testModelNameWithoutLoadingModel() {
        IMPDecisionModel decisionModel = new IMPDecisionModel("music");
        assertEquals("music", decisionModel.getModelName());
    }

    @Test
    public void testModelName() throws MalformedURLException {
        URL url = new URL(ModelURL);
        IMPDecisionModel decisionModel = IMPDecisionModel.load(url);
        IMPLog.d(Tag, "modelName=" + decisionModel.getModelName());
        assertEquals("dummy-model-0", decisionModel.getModelName());
    }

    @Test
    public void testGet() throws MalformedURLException {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        URL url = new URL(ModelURL);
        String greeting = (String) IMPDecisionModel.load(url).chooseFrom(variants).get();
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
        String greeting = (String) new IMPDecisionModel("").chooseFrom(variants).get();
        IMPLog.d(Tag, "greeting=" + greeting);
        assertNotNull(greeting);
    }

    @Test
    public void testLoadAsync() throws MalformedURLException, InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        URL url = new URL(ModelURL);
        IMPDecisionModel decisionModel = new IMPDecisionModel("music");
        decisionModel.loadAsync(url, new IMPDecisionModel.IMPDecisionModelLoadListener() {
            @Override
            public void onFinish(ImprovePredictor predictor) {
                assertTrue(Looper.myLooper() == Looper.getMainLooper());
                assertNotNull(predictor);
                IMPLog.d(Tag, "testLoadAsync, OK");
                semaphore.release();
            }
        });
        semaphore.acquire();
    }

    @Test
    public void testLoadGzipModel() throws MalformedURLException {
        URL url = new URL(CompressedModelURL);
        IMPDecisionModel decisionModel = IMPDecisionModel.load(url);
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
    public void testLoadLocalModel() throws IOException {
        // Download model file and save it to external cache dir
        String localModelFilePath = download();
        URL url = new File(localModelFilePath).toURI().toURL();

        IMPDecisionModel decisionModel = IMPDecisionModel.load(url);
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


    private String download() throws IOException, SecurityException {
        URL url = new URL(ModelURL);
        InputStream is = new BufferedInputStream(url.openStream());
        byte[] buffer = new byte[1024];
        int length;

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String absfile = appContext.getExternalCacheDir() + "/" + UUID.randomUUID().toString() + ".xgb";
        IMPLog.d(Tag, "cache file path: " + absfile);

        // write to cache
        FileOutputStream fos = new FileOutputStream(new File(absfile));
        while ((length = is.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        return absfile;
    }
}
