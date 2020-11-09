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
    private Map<String, Object> context;

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


    public static Map<String, Object> simpleContext() {
        return new HashMap<String, Object>();
    }


    public DecisionMaker(List<Object> variants, DecisionModel model) {
        this(variants, model, null);
    }

    public DecisionMaker(List<Object> variants, DecisionModel model, Map<String, Object> context) {
        this(variants, model, model.getModelName(), context);
    }


    public DecisionMaker(List<Object> variants, String modelName) {
        this(variants, modelName, null);
    }

    public DecisionMaker(List<Object> variants, String modelName, Map<String, Object> context) {
        this(variants, null, modelName, context);
    }

    DecisionMaker(List<Object> variants, DecisionModel model, String modelName, Map<String, Object> context) {
        this.variants = Collections.unmodifiableList(variants);
        this.model = model;
        this.modelName = modelName;
        this.context = context != null ? context : DecisionMaker.simpleContext();
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
    public Map<String, Object> getContext() {
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
        List<ScoredVariant> scoredVariants = scored();
        List<Number> result = new ArrayList<>();
        for (ScoredVariant sv : scoredVariants) {
            result.add(sv.getScore());
        }
        this.localScores = Collections.unmodifiableList(result);
        return this.localScores;
    }

    private List<ScoredVariant> generateScoresForRankedVariants() {
        Double[] scores = new Double[variants.size()];

        for (int i = 0; i < variants.size(); ++i) {
            scores[i] = randomGenerator.nextGaussian();
        }
        Arrays.sort(scores);
        List<ScoredVariant> result = new ArrayList<>(variants.size());
        for (int i = 0; i < variants.size(); ++i) {
            result.add(new ScoredVariant(variants.get(i), scores[i]));
        }
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
        Collections.sort(scoredVariants);
        List<Object> ranked = new ArrayList<>(scoredVariants.size());
        for (ScoredVariant v : scoredVariants) {
            ranked.add(v.getVariant());
        }
        this.localRanked = Collections.unmodifiableList(ranked);
        return localRanked;
    }

    @Override
    public List<ScoredVariant> scored() {
        if (localScored != null) {
            return localScored;
        }
        if (model != null) {
            this.localScored = Collections.unmodifiableList(model.score(variants, context));
        } else {
            this.localScored = generateScoresForRankedVariants();
        }
        return this.localScored;
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
