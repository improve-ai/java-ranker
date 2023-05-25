package ai.improve;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;

@RunWith(AndroidJUnit4.class)
public class TestRewardTracker {
    public static final String Tag = "TestRewardTracker";

    static URL trackUrl() throws MalformedURLException {
        return new URL("https://f6f7vxez5b5u25l2pw6qzpr7bm0qojug.lambda-url.us-east-2.on.aws/");
    }

    static String trackApiKey = "";

    RewardTracker tracker() throws MalformedURLException {
        return new RewardTracker("greetings", trackUrl(), trackApiKey);
    }

    @Test
    public void testTrack_random_sample() throws MalformedURLException {
        RewardTracker tracker = tracker();
        List<Integer> candidates = Arrays.asList(1, 2, 3);
        candidates.remove(0);
//        tracker.track(1, candidates);
        assertEquals(2, candidates.size());
    }

    @Test
    public void testXX() {
        Map<String, Integer> s = new HashMap();
        s.put("hello", null);
        IMPLog.d(Tag, "");
    }
}
