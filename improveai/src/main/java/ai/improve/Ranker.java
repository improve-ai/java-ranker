package ai.improve;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import ai.improve.util.Utils;

public class Ranker {
    private final Scorer scorer;

    public Ranker(Scorer scorer) {
        this.scorer = scorer;
    }

    /**
     * @param modelUrl URL of a plain or gzip compressed xgb model. Could be like:
     *                 https://improve.ai/model.xgb
     *                 https://improve.ai/model.xgb.gz
     *                 file:///android_asset/models/model.xgb(Bundled models in assets folder)
     */
    public Ranker(URL modelUrl) throws IOException, InterruptedException {
        this.scorer = new Scorer(modelUrl);
    }

    /**
     * Rank the list of items by their scores.
     * @param items The list of items to rank.
     */
    public <T> List<T> rank(List<T> items) {
        return rank(items, null);
    }

    /**
     * Rank the list of items by their scores.
     * @param items The list of items to rank.
     * @param context Extra JSON encodable context info that will be used with each of the item to get its score.
     */
    public <T> List<T> rank(List<T> items, Object context) {
        List<Double> scores = scorer.score(items, context);
        return Utils.rank(items, scores);
    }
}
