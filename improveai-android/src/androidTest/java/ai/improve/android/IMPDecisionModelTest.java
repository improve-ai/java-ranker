package ai.improve.android;

import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import ai.improve.android.xgbpredictor.ImprovePredictor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class IMPDecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    private static final String ModelURL = "https://yamotek-1251356641.cos.ap-guangzhou.myqcloud.com/dummy_v6.xgb";

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
}
