package ai.improve.android;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

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
        IMPLog.d(Tag, "testHistoryId, historyId=" + tracker_0.getHistoryId());
        assertNotNull(tracker_0.getHistoryId());

        IMPDecisionTracker tracker_1 = new IMPDecisionTracker(appContext, "");
        assertEquals(tracker_0.getHistoryId(), tracker_1.getHistoryId());
    }
}
