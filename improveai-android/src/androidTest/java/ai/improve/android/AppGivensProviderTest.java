package ai.improve.android;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ai.improve.IMPLog;

import static ai.improve.android.AppGivensProviderImp.SP_Key_Born_Time;
import static ai.improve.android.AppGivensProviderImp.SP_Key_Session_Count;
import static ai.improve.android.Constants.SP_File_Name;
import static ai.improve.android.DecisionModelTest.ModelURL;
import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AppGivensProviderTest {
    public static final String Tag = "AppGivensProviderTest";

    static {
        IMPLog.setLogger(new LoggerImp());
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testSessionCountFirst() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Remove previous session count
        SharedPreferences sp = context.getSharedPreferences(SP_File_Name, Context.MODE_PRIVATE);
        sp.edit().remove(SP_Key_Session_Count).apply();

        AppGivensProviderImp appGivensProvider = new AppGivensProviderImp(context);

        assertEquals(0, AppGivensProviderUtils.getSessionCount(context));
    }

    /**
     * testSessionCountSecond must be run after testSessionCountFirst in a different session, otherwise
     * they would be considered as one session and testSessionCountSecond would fail.
     * */
    @Test
    public void testSessionCountSecond() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppGivensProviderImp appGivensProvider = new AppGivensProviderImp(context);
        assertEquals(1, AppGivensProviderUtils.getSessionCount(context));
    }

    @Test
    public void testDecisionCount() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int decisionCount = AppGivensProviderUtils.getDecisionCount(context);

        URL modelUrl = new URL(ModelURL);
        Map<String, Object> given = new HashMap<>();
        given.put("language", "cowboy");

        DecisionModel.load(modelUrl)
                .addGivensProvider(new AppGivensProviderImp(context))
                .chooseFrom(Arrays.asList("Hello World", "Howdy World", "Yo World"))
                .given(given)
                .get();

        assertEquals(decisionCount+1, AppGivensProviderUtils.getDecisionCount(context));
    }

    @Test
    public void testSinceSessionStart() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppGivensProviderImp appGivensProvider = new AppGivensProviderImp(context);
        Thread.sleep(3000);
        double t = AppGivensProviderUtils.getSinceSessionStart(context);
        IMPLog.d(Tag, "sinceSessionStart:" + t);
        assertEquals(t, 3.0, 0.1);
    }

    /**
     * AppGivensProviderImp instances in one test case are in the same session
     * I don't know how to unit test this one...
     * Run it twice manually??
     * */
    @Test
    public void testSinceLastSessionStart() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppGivensProviderImp appGivensProvider = new AppGivensProviderImp(context);
        double t = AppGivensProviderUtils.getSinceLastSessionStart();
        IMPLog.d(Tag, "sinceLastSessionStart: " + t);
    }

    @Test
    public void testSinceBorn() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Remove previous born time
        SharedPreferences sp = context.getSharedPreferences(SP_File_Name, Context.MODE_PRIVATE);
        sp.edit().remove(SP_Key_Born_Time).apply();

        AppGivensProviderImp appGivensProvider = new AppGivensProviderImp(context);
        Thread.sleep(10 * 1000);
        double t = AppGivensProviderUtils.getSinceBorn(context);
        assertEquals(t, 10.0, 0.1);

        Thread.sleep(10 * 1000);
        t = AppGivensProviderUtils.getSinceBorn(context);
        assertEquals(t, 20.0, 0.1);
    }

    @Test
    public void testParseDeviceVersion() {
        assertEquals(5001, AppGivensProviderUtils.parseDeviceVersion("Nokia 5.1 Plus"));
        assertEquals(5000, AppGivensProviderUtils.parseDeviceVersion("Nokia 5. Plus"));
        assertEquals(5000, AppGivensProviderUtils.parseDeviceVersion("Nokia 5..1 Plus"));
        assertEquals(0, AppGivensProviderUtils.parseDeviceVersion("Nokia XL"));
        assertEquals(5000, AppGivensProviderUtils.parseDeviceVersion("Nubia Z5S mini NX403A"));
        assertEquals(11001, AppGivensProviderUtils.parseDeviceVersion("Nokia 11.1 Plus"));
        assertEquals(11001, AppGivensProviderUtils.parseDeviceVersion("Nokia 11.1.2 Plus"));
    }

    @Test
    public void testVersionToInt() {
        assertEquals(6001.123, AppGivensProviderUtils.versionToInt("6.1.123"), 0.1);
        assertEquals(6001.123, AppGivensProviderUtils.versionToInt("6.1.123.abc"), 0.1);
        assertEquals(6001, AppGivensProviderUtils.versionToInt("6.1"), 0.1);
        assertEquals(6000, AppGivensProviderUtils.versionToInt("6"), 0.1);
        assertEquals(0, AppGivensProviderUtils.versionToInt("a.1.0"), 0);
    }
}