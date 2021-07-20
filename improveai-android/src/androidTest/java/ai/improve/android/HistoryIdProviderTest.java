package ai.improve.android;


import org.junit.Test;

import ai.improve.IMPLog;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HistoryIdProviderTest {
    public static final String Tag = "HistoryIdProviderTest";

    @Test
    public void testHistoryId() {
        HistoryIdProviderImp provider = new HistoryIdProviderImp();
        String historyId = provider.getHistoryId();
        IMPLog.d(Tag, "historyId=" + historyId);
        assertNotNull(historyId);
        assertTrue(historyId.length() > 0);
    }
}
