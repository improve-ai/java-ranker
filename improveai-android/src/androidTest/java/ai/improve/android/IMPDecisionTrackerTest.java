package ai.improve.android;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

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
    public void testHistoryId() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        IMPDecisionTracker tracker_0 = new IMPDecisionTracker(appContext, "");

        String historyId_0 = (String) getFieldValue(tracker_0, "historyId");
        IMPLog.d(Tag, "testHistoryId, historyId=" + historyId_0);
        assertNotNull(historyId_0);

        IMPDecisionTracker tracker_1 = new IMPDecisionTracker(appContext, "");
        String historyId_1 = (String) getFieldValue(tracker_1, "historyId");
        assertEquals(historyId_0, historyId_1);
    }

    private Object getFieldValue(Object object, String fieldName){
        Field field = getDeclaredField(object, fieldName) ;
        field.setAccessible(true) ;
        try {
            return field.get(object) ;
        } catch(Exception e) {
            e.printStackTrace() ;
        }
        return null;
    }

    private Field getDeclaredField(Object object, String fieldName){
        Field field = null ;
        Class<?> clazz = object.getClass() ;
        for(; clazz != Object.class ; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName) ;
                return field ;
            } catch (Exception e) {
            }
        }
        return null;
    }
}
