package ai.improve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static ai.improve.TestModelValidation.getContext;
import static ai.improve.TestModelValidation.toMap;
import static ai.improve.android.Constants.Improve_SP_File_Name;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.ksuid.KsuidGenerator;
import ai.improve.log.IMPLog;
import ai.improve.util.HttpUtil;
import ai.improve.util.Utils;

@RunWith(AndroidJUnit4.class)
public class TestRewardTracker {
    public static final String Tag = "TestRewardTracker";

    public static final String RequestBodyKey = "improve.ai.trackRequestBody";

    static URL trackUrl() throws MalformedURLException {
        return new URL("https://f6f7vxez5b5u25l2pw6qzpr7bm0qojug.lambda-url.us-east-2.on.aws/");
    }

    static String trackApiKey = "api-key";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
        HttpUtil.writeBody = true;
    }

    RewardTracker tracker() throws MalformedURLException {
        return new RewardTracker("greetings", trackUrl(), trackApiKey);
    }

    @Test
    public void testConstructor() throws MalformedURLException {
        RewardTracker tracker = new RewardTracker("greetings", trackUrl(), trackApiKey);
        assertEquals("greetings", tracker.getModelName());
        assertEquals(trackUrl(), tracker.getTrackURL());
        assertEquals(trackApiKey, tracker.getTrackApiKey());
    }

    @Test
    public void testConstructor_null_trackUrl() {
        try {
            new RewardTracker("greetings", null, null);
            fail("trackUrl can't be null");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConstructor_empty_modelName() throws MalformedURLException {
        try {
            new RewardTracker("", trackUrl(), null);
            fail("invalid model name");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConstructor_null_modelName() throws MalformedURLException {
        try {
            new RewardTracker(null, trackUrl(), null);
            fail("invalid model name");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testModelName() {
        assertTrue(Utils.isValidModelName("a"));
        assertTrue(Utils.isValidModelName("a_"));
        assertTrue(Utils.isValidModelName("a."));
        assertTrue(Utils.isValidModelName("a-"));
        assertTrue(Utils.isValidModelName("a1"));
        assertTrue(Utils.isValidModelName("3abb"));

        assertFalse(Utils.isValidModelName("_a"));
        assertFalse(Utils.isValidModelName("a+"));
        assertFalse(Utils.isValidModelName("a\\"));
        assertFalse(Utils.isValidModelName(".a"));

        String name = "";
        for(int i = 0; i < 64; ++i) {
            name += "a";
        }
        assertEquals(64, name.length());
        assertTrue(Utils.isValidModelName(name));

        name += "a";
        assertEquals(65, name.length());
        assertFalse(Utils.isValidModelName(name));
    }

    @Test
    public void testTrack() throws MalformedURLException, JSONException {
        String rewardId = tracker().track(1, Arrays.asList(1, 2, 3));
        assertEquals(KsuidGenerator.KSUID_STRING_LENGTH, rewardId.length());

        SharedPreferences sp = getContext().getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject(sp.getString(RequestBodyKey, ""));
        assertEquals(toMap(root).size(), 6);
        assertEquals(1, root.getInt("item"));
        assertEquals(3, root.getInt("count"));
        assertEquals("greetings", root.getString("model"));
        assertEquals("decision", root.getString("type"));
        int sample = root.getInt("sample");
        assertTrue(sample == 2 || sample == 3);
        assertEquals(27, root.getString("message_id").length());
    }

    @Test
    public void testTrack_null_candidates() throws MalformedURLException {
        try {
            tracker().track(null, null);
            fail("candidates can't be null.");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        try {
            tracker().track(null, null, "context");
            fail("candidates can't be null.");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTrack_null_item() throws MalformedURLException, JSONException {
        tracker().track(null, Arrays.asList(null, null, null));
        SharedPreferences sp = getContext().getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject(sp.getString(RequestBodyKey, ""));
        assertEquals(toMap(root).size(), 6);
        assertTrue(root.isNull("item"));
        assertTrue(root.isNull("sample"));
        assertEquals(3, root.getInt("count"));
    }

    @Test
    public void testTrack_context() throws MalformedURLException, JSONException {
        tracker().track(1, Arrays.asList(1, 2, 3), 1);

        SharedPreferences sp = getContext().getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject(sp.getString(RequestBodyKey, ""));
        assertEquals(toMap(root).size(), 7);
        assertEquals(1, root.getInt("item"));
        assertEquals(3, root.getInt("count"));
        assertEquals("greetings", root.getString("model"));
        assertEquals("decision", root.getString("type"));
        int sample = root.getInt("sample");
        assertTrue(sample == 2 || sample == 3);
        assertEquals(27, root.getString("message_id").length());
        assertEquals(1, root.getInt("context"));
    }

    @Test
    public void testTrack_not_json_encodable() throws MalformedURLException {
        try {
            tracker().track(1, Arrays.asList(1, 2, 3), new Date());
            fail("item/sample/context must be JSON encodable");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTrack_random_sample() throws MalformedURLException, JSONException {
        URL trackUrl = new URL("https://xxxx"); // dummy url doesn't matter here.
        RewardTracker tracker = new RewardTracker("greetings", trackUrl, trackApiKey);
        List<Integer> candidates = Arrays.asList(1, 2, 3);
        int count2 = 0;
        int count3 = 0;
        int loop = 10000;
        for(int i = 0; i < loop; ++i) {
            tracker.track(1, candidates);

            SharedPreferences sp = getContext().getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
            JSONObject root = new JSONObject(sp.getString(RequestBodyKey, ""));
            int sample = root.getInt("sample");
            assertTrue(sample == 2 || sample == 3);
            if(sample == 2) {
                count2++;
            } else {
                count3++;
            }
        }
        IMPLog.d(Tag, "count2 = " + count2 + ", count3 = " + count3);
        assertEquals(loop, count2 + count3);
        assertTrue(Math.abs(count2 - count3) < (loop/2 * 0.03));
    }

    @Test
    public void testTrackWithSample() throws MalformedURLException, JSONException {
        tracker().trackWithSample("hi", "hello", 3);

        SharedPreferences sp = getContext().getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject(sp.getString(RequestBodyKey, ""));
        assertEquals(toMap(root).size(), 6);
        assertEquals("hi", root.getString("item"));
        assertEquals(3, root.getInt("count"));
        assertEquals("greetings", root.getString("model"));
        assertEquals("decision", root.getString("type"));
        assertEquals("hello", root.getString("sample"));
        assertEquals(27, root.getString("message_id").length());
    }

    @Test
    public void testTrackWithSample_context() throws MalformedURLException, JSONException {
        Map<String, String> context = new HashMap<>();
        context.put("lang", "en");
        tracker().trackWithSample("hi", "hello", 3, context);

        SharedPreferences sp = getContext().getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject(sp.getString(RequestBodyKey, ""));
        assertEquals(toMap(root).size(), 7);
        assertEquals("hi", root.getString("item"));
        assertEquals(3, root.getInt("count"));
        assertEquals("greetings", root.getString("model"));
        assertEquals("decision", root.getString("type"));
        assertEquals("hello", root.getString("sample"));
        assertEquals(27, root.getString("message_id").length());

        JSONObject contextJSONObject = root.getJSONObject("context");
        assertEquals("en", contextJSONObject.getString("lang"));
    }

    @Test
    public void testTrackWithSample_numCandidates() throws MalformedURLException {
        try {
            tracker().trackWithSample("hi", "hello", 1);
            fail("numCandidates must not be smaller than 2");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTrackWithSample_not_json_encodable() throws MalformedURLException {
        try {
            tracker().trackWithSample("hi", "hello", 2, new Date());
            fail("item/sample/context must be JSON encodable!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
