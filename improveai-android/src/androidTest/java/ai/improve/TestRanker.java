package ai.improve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ai.improve.TestScorer.DummyV8ModelUrl;
import static ai.improve.TestScorer.Tag;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import ai.improve.log.IMPLog;
import ai.improve.util.Utils;

public class TestRanker {
    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testRanker_constructor_scorer() throws IOException, InterruptedException {
        Scorer scorer = new Scorer(new URL(DummyV8ModelUrl));
        new Ranker(scorer);
    }

    @Test
    public void testRanker_constructor_url() throws IOException, InterruptedException {
        new Ranker(new URL(DummyV8ModelUrl));
    }

    @Test
    public void testRank() throws IOException, InterruptedException {
        Ranker ranker = new Ranker(new URL(DummyV8ModelUrl));

        List<Integer> rankedIntegers = ranker.rank(Arrays.asList(1, 2, 3));
        IMPLog.d(Tag, "ranked integers: " + rankedIntegers);
        assertEquals(3, rankedIntegers.size());

        List<String> rankedStrings = ranker.rank(Arrays.asList("a", "b", "c", "d"));
        IMPLog.d(Tag, "ranked strings: " + rankedStrings);
        assertEquals(4, rankedStrings.size());
    }

    @Test
    public void testRank_null_items() throws IOException, InterruptedException {
        Ranker ranker = new Ranker(new URL(DummyV8ModelUrl));
        try {
            ranker.rank(null);
            fail("items can't be null");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
    }

    @Test
    public void testRank_empty_items() throws IOException, InterruptedException {
        Ranker ranker = new Ranker(new URL(DummyV8ModelUrl));
        try {
            ranker.rank(new ArrayList<>());
            fail("items can't be empty");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
    }

    @Test
    public void testRank_with_context() throws IOException, InterruptedException {
        Ranker ranker = new Ranker(new URL(DummyV8ModelUrl));

        List<Integer> rankedIntegers = ranker.rank(Arrays.asList(1, 2, 3), "context");
        IMPLog.d(Tag, "ranked integers: " + rankedIntegers);

        List<String> rankedStrings = ranker.rank(Arrays.asList("a", "b", "c"), 1);
        IMPLog.d(Tag, "ranked strings: " + rankedStrings);
    }

    @Test
    public void testRank_with_context_null_items() throws IOException, InterruptedException {
        Ranker ranker = new Ranker(new URL(DummyV8ModelUrl));
        try {
            ranker.rank(null, 1);
            fail("items can't be empty");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
    }

    @Test
    public void testRank_with_context_empty_items() throws IOException, InterruptedException {
        Ranker ranker = new Ranker(new URL(DummyV8ModelUrl));
        try {
            ranker.rank(new ArrayList<>(), "context");
            fail("items can't be empty");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }
    }

    @Test
    public void testRankWithScores() {
        int count = 100;
        List<Integer> variants = new ArrayList();
        List<Double> scores = new ArrayList<>();

        for(int i = 0; i < count; ++i) {
            variants.add(i);
            scores.add((double)i);
        }

        Random random = new Random();
        // shuffle
        for(int i = 0; i < 100; ++i) {
            int m = random.nextInt(count);
            int n = random.nextInt(count);
            Collections.swap(variants, m, n);
            Collections.swap(scores, m, n);
        }
        IMPLog.d(Tag, "Shuffled.....");
        for(int i = 0; i < variants.size(); ++i) {
            IMPLog.d(Tag, "" + variants.get(i) + ", " + scores.get(i));
        }

        IMPLog.d(Tag, "Sorted.....");
        List<Integer> sorted = Utils.rank(variants, scores);
        assertEquals(sorted.size(), variants.size());

        for(int i = 0; i < sorted.size(); ++i) {
            IMPLog.d(Tag, "" + sorted.get(i));
            if(i != variants.size()-1) {
                assertTrue(sorted.get(i) > sorted.get(i+1));
            }
        }
    }
}
