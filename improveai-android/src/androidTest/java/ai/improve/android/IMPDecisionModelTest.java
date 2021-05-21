package ai.improve.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IMPDecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    private static final String ModelURL = "https://yamotek-1251356641.cos.ap-guangzhou.myqcloud.com/dummy_v6.xgb";

    @Test
    public void testLoad() throws MalformedURLException {
        IMPLog.setLogger(new IMPLoggerImp());
        IMPLog.enableLogging(true);

        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        URL url = new URL(ModelURL);
        String greeting = (String) IMPDecisionModel.load(url).chooseFrom(variants).get();
        IMPLog.d(Tag, "greeting=" + greeting);
        assertNotNull(greeting);
    }
}
