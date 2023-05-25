package ai.improve;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import ai.improve.util.Utils;

public class Ranker {
    private Scorer scorer;

    public Ranker(Scorer scorer) {
        this.scorer = scorer;
    }

    public Ranker(URL modelUrl) throws IOException, InterruptedException {
        this.scorer = new Scorer(modelUrl);
    }

    public <T> List<T> rank(List<T> items) {
        return rank(items, null);
    }

    public <T> List<T> rank(List<T> items, Object context) {
        List<Double> scores = scorer.score(items, context);
        return Utils.rank(items, scores);
    }
}
