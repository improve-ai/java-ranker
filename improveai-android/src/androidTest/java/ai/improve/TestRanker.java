package ai.improve;

import static ai.improve.TestScorer.DummyV8ModelUrl;
import static ai.improve.TestScorer.Tag;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import ai.improve.log.IMPLog;

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

        List<String> rankedStrings = ranker.rank(Arrays.asList("a", "b", "c"));
        IMPLog.d(Tag, "ranked strings: " + rankedStrings);
    }

    @Test
    public void testRank_with_context() throws IOException, InterruptedException {
        Ranker ranker = new Ranker(new URL(DummyV8ModelUrl));

        List<Integer> rankedIntegers = ranker.rank(Arrays.asList(1, 2, 3), "context");
        IMPLog.d(Tag, "ranked integers: " + rankedIntegers);

        List<String> rankedStrings = ranker.rank(Arrays.asList("a", "b", "c"), 1);
        IMPLog.d(Tag, "ranked strings: " + rankedStrings);
    }
}
