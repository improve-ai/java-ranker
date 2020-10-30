package ai.improve.android.spi;

import ai.improve.android.Decision;
import ai.improve.android.DecisionModel;
import ai.improve.android.ScoredVariant;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

public class DecisionMaker implements Decision {


    private static final int DEFAULT_MAX_RUNNERSUP = 50;


    private List<Object> variants;
    private DecisionModel model;

    private String modelName;
    private Map context;

    private int maxRunnersUp = DEFAULT_MAX_RUNNERSUP;

    private boolean trackRunnersUp;

    /**
     * Local (memoized) copy of scores
     **/
    private List<? extends Number> localScores;
    private List<Object> localRanked;
    private Object localBest;
    private List<ScoredVariant> localScored;


    private RandomGenerator randomGenerator = new JDKRandomGenerator();


    public static Map simpleContext() {
        return new HashMap();
    }


    public DecisionMaker(List<Object> variants, DecisionModel model) {
        this.variants = variants;
        this.model = model;
        this.context = DecisionMaker.simpleContext();
        this.trackRunnersUp = isTrackRunnersUpEnabled();
    }

    public DecisionMaker(List<Object> variants, DecisionModel model, Map context) {
        this.variants = variants;
        this.model = model;
        this.context = context;
        this.trackRunnersUp = isTrackRunnersUpEnabled();

    }


    public DecisionMaker(List<Object> variants, String modelName) {
        this.variants = variants;
        this.modelName = modelName;
        this.context = DecisionMaker.simpleContext();
        this.trackRunnersUp = isTrackRunnersUpEnabled();

    }

    public DecisionMaker(List<Object> variants, String modelName, Map context) {
        this.variants = variants;
        this.modelName = modelName;
        this.context = context;
        this.trackRunnersUp = isTrackRunnersUpEnabled();

    }

    public String getModelName() {
        if (model != null) {
            return model.getModelName();
        }
        return modelName;
    }



    public void setMaxRunnersUp(int maxRunnersUp) {
        this.maxRunnersUp = maxRunnersUp;
    }

    @Override
    public List<Object> getVariants() {
        return variants;
    }

    @Override
    public Map getContext() {
        return context;
    }

    @Override
    public boolean isTrackRunnersUp() {
        return trackRunnersUp;
    }

    private boolean isTrackRunnersUpEnabled() {
        return randomGenerator.nextDouble() < (1.0 / (double) (Math.min(variants.size() - 1, maxRunnersUp)));
    }

    @Override
    public List<? extends Number> scores() {
        if (localScores != null) {
            return localScores;
        }
        if (model != null) {
            this.localScores = model.score(variants, context);
        } else {
            this.localScores = generateScoresForRankedVariants();
        }
        return this.localScores;
    }

    private List<? extends Number> generateScoresForRankedVariants() {
        Double[] scores = new Double[variants.size()];

        for (int i = 0; i < variants.size(); ++i) {
            scores[i] = randomGenerator.nextGaussian();
        }
        List<Double> result = Arrays.asList(scores);
        Collections.sort(result);
        return result;
    }

    @Override
    public List<Object> ranked() {
        if (model == null) {
            return variants;
        }
        if (localRanked != null) {
            return localRanked;
        }
        List<ScoredVariant> scoredVariants = scored();
        Collections.sort(scoredVariants, new Comparator<ScoredVariant>() {
            @Override
            public int compare(ScoredVariant lhs, ScoredVariant rhs) {
                return Double.compare(lhs.getScore(), rhs.getScore());
            }
        });
        List<Object> ranked = new ArrayList<>(scoredVariants.size());
        for (ScoredVariant v : scoredVariants) {
            ranked.add(v.getVariant());
        }
        this.localRanked = ranked;
        return localRanked;
    }

    @Override
    public List<ScoredVariant> scored() {
        if (localScored != null) {
            return localScored;
        }
        List<? extends Number> scores = scores();
        List<ScoredVariant> result = new ArrayList<>();
        for (int i = 0; i < variants.size(); ++i) {
            result.add(new ScoredVariant(variants.get(i), scores.get(i).doubleValue()));
        }
        this.localScored = result;
        return result;
    }

    @Override
    public Object best() {
        if (this.localBest != null) {
            return this.localBest;
        }
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        if (model == null) {
            this.localBest = variants.get(0);
            return this.localBest;
        }
        List<Object> ranked = ranked();
        if (ranked != null && !ranked.isEmpty()) {
            this.localBest = ranked.get(0);
            return this.localBest;
        } else {
            return null;
        }
    }

    @Override
    public List<Object> topRunnersUp() {
        List<Object> ranked = ranked();
        return ranked.subList(1, Math.min(ranked.size() - 1, maxRunnersUp));
    }
}
