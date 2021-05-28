package ai.improve.android;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import ai.improve.IMPLog;
import ai.improve.IMPTrackerHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IMPDecisionTrackerTest {
    private static final String Tag = "IMPDecisionTrackerTest";

    static {
        IMPLog.setLogger(new IMPLoggerImp());
        IMPLog.enableLogging(true);
    }

    @Test
    public void testHistoryId() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionTracker tracker_0 = new IMPDecisionTracker(appContext, "");

        String historyId_0 = IMPTrackerHandler.getHistoryId();
        IMPLog.d(Tag, "testHistoryId, historyId=" + historyId_0);
        assertNotNull(historyId_0);

        IMPDecisionTracker tracker_1 = new IMPDecisionTracker(appContext, "");
        String historyId_1 = (String) IMPTrackerHandler.getHistoryId();
        assertEquals(historyId_0, historyId_1);
    }
}
