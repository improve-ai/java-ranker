package ai.improve.android;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ai.improve.DecisionModel;
import ai.improve.log.IMPLog;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppGivensProviderTest {

    private Context context;

    @Before
    public void setUp() {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testOverlappingGivensKey() {
        Map userGivens = new HashMap();
        userGivens.put(AppGivensProvider.APP_Given_Key_Language, "hi");

        Map givens = new AppGivensProvider(context).givensForModel(new DecisionModel("hello"), userGivens);
        assertNotNull(givens);
        assertEquals("hi", givens.get(AppGivensProvider.APP_Given_Key_Language));
    }
}
