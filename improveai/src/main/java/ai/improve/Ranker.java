package ai.improve;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import ai.improve.util.Utils;

/**
 * A utility for ranking items based on their scores. The Ranker struct takes a Improve AI model to evaluate and rank the given items.
 */
public class Ranker {
    /**
     * A Scorer is used to calculate scores for items. Items must be JSON encodable.
     */
    private final Scorer scorer;

    /**
     * Create a Ranker instance with a CoreML model.
     * @param scorer a Scorer object to be used with this Ranker
     */
    public Ranker(Scorer scorer) {
        this.scorer = scorer;
    }

    /**
     * Create a Ranker instance with a CoreML model.
     * @param modelUrl URL of a plain or gzip compressed CoreML model resource
     *                 https://improve.ai/model.xgb
     *                 https://improve.ai/model.xgb.gz
     *                 file:///android_asset/models/model.xgb(Bundled models in assets folder)
     * @throws IOException, InterruptedException if there is an issue initializing the Scorer with the modelUrl.
     */
    public Ranker(URL modelUrl) throws IOException, InterruptedException {
        this.scorer = new Scorer(modelUrl);
    }

    /**
     * Rank the list of items by their scores.
     * @param items the list of items to rank.
     * @return List<T> -> a list of ranked items, sorted by their scores in descending order.
     */
    public <T> List<T> rank(List<T> items) {
        return rank(items, null);
    }

    /**
     * Rank the list of items by their scores.
     * @param items the list of items to rank.
     * @param context extra context info that will be used with each of the item to get its score.
     * @return List<T> -> a list of ranked items, sorted by their scores in descending order.
     */
    public <T> List<T> rank(List<T> items, Object context) {
        List<Double> scores = scorer.score(items, context);
        return Utils.rank(items, scores);
    }
}
