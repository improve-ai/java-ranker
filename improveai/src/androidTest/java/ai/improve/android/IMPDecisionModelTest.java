package ai.improve.android;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;


import static org.junit.Assert.assertNotNull;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class IMPDecisionModelTest {
    public static final String Tag = "IMPDecisionModelTest";

    @Test
    public void testLoad() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionModel model = IMPDecisionModel.loadFromAsset(appContext, "dummy_v6.xgb");
        assertNotNull(model);
    }
}