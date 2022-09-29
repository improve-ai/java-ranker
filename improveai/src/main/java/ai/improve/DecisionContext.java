package ai.improve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ai.improve.util.ModelUtils;

public class DecisionContext {

    private final DecisionModel decisionModel;

    private final Map<String, ?> givens;

    protected DecisionContext(DecisionModel decisionModel, Map<String, ?> givens) {
        this.decisionModel = decisionModel;
        this.givens = givens;
    }

    /**
     * @see ai.improve.DecisionModel#score(List)
     */
    public <T> List<Double> score(List<T> variants) {
        Map<String, ?> allGivens = decisionModel.combinedGivens(givens);
        return decisionModel.scoreInternal(variants, allGivens);
    }

    /**
     * @see ai.improve.DecisionModel#decide(List)
     */
    public <T> Decision<T> decide(List<T> variants) {
        return decide(variants, false);
    }

    /**
     * @see ai.improve.DecisionModel#decide(List)
     */
    public <T> Decision<T> decide(List<T> variants, boolean ordered) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants to choose from can't be null or empty");
        }

        Map<String, ?> allGivens = decisionModel.combinedGivens(givens);
        List<T> rankedVariants;
        if(ordered) {
            rankedVariants = new ArrayList<>(variants);
        } else {
            if(decisionModel.isLoaded()) {
                List<Double> scores = decisionModel.scoreInternal(variants, allGivens);
                rankedVariants = DecisionModel.rank(variants, scores);
            } else {
                rankedVariants = new ArrayList<>(variants);
            }
        }
        return new Decision<>(decisionModel, rankedVariants, allGivens);
    }

    /**
     * @see ai.improve.DecisionModel#decide(List, List)
     */
    public <T> Decision<T> decide(List<T> variants, List<Double> scores) {
        Map<String, ?> allGivens = decisionModel.combinedGivens(givens);
        List<T> rankedVariants = DecisionModel.rank(variants, scores);
        return new Decision<>(decisionModel, rankedVariants, allGivens);
    }

    /**
     * @see ai.improve.DecisionModel#which(Object[])
     */
    @SafeVarargs
    public final <T> T which(T... variants) {
        if(variants == null || variants.length <= 0) {
            throw new IllegalArgumentException("should at least provide one variant.");
        }
        Decision<T> decision = decide(Arrays.asList(variants));
        decision.track(decisionModel.getTracker());
        return decision.get();
    }

    /**
     * @see ai.improve.DecisionModel#which(Object[])
     */
    public <T> T whichFrom(List<T> variants) {
        Decision<T> decision = decide(variants);
        decision.track(decisionModel.getTracker());
        return decision.get();
    }

    /**
     * @see ai.improve.DecisionModel#rank(List) 
     */
    public <T> List<T> rank(List<T> variants) {
        return decide(variants).ranked;
    }

    /**
     * @see ai.improve.DecisionModel#optimize(Map) 
     */
    public Map<String, Object> optimize(Map<String, ?> variantMap) {
        return whichFrom(decisionModel.fullFactorialVariants(variantMap));
    }

    protected String track(Object variant, List<?> runnersUp, Object sample, int samplePoolSize) {
        if(samplePoolSize < 0) {
            throw new IllegalArgumentException("samplePoolSize can't be smaller than 0!");
        }

        if(samplePoolSize == 0 && sample != null) {
            throw new IllegalArgumentException("sample pool of size 0 can't produce a nonnull sample!");
        }

        DecisionTracker tracker = decisionModel.getTracker();
        if(tracker == null) {
            throw new IllegalStateException("trackURL of the DecisionModel is null!");
        }

        Map<String, ?> allGivens = decisionModel.combinedGivens(givens);

        return tracker.track(variant, allGivens, runnersUp, sample, samplePoolSize, decisionModel.getModelName());
    }

    /**
     * @see ai.improve.DecisionModel#chooseFrom(List)
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public <T> Decision<T> chooseFrom(List<T> variants) {
        return decide(variants);
    }

    /**
     * @see ai.improve.DecisionModel#chooseFrom(List, List)
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public <T> Decision<T> chooseFrom(List<T> variants, List<Double> scores) {
        return decide(variants, scores);
    }

    /**
     * @see ai.improve.DecisionModel#chooseFrom(List)
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public <T> Decision<T> chooseFirst(List<T> variants) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants can't be null or empty");
        }
        return decide(variants, ModelUtils.generateDescendingGaussians(variants.size()));
    }

    /**
     * @see ai.improve.DecisionModel#first(List)
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public <T> T first(List<T> variants) {
        return chooseFirst(variants).get();
    }

    /**
     * @see ai.improve.DecisionModel#first(Object...)
     * @deprecated Remove in 8.0.
     */
    @SafeVarargs
    @Deprecated
    public final <T> T first(T... variants) {
        Decision<T> decision = chooseFirst(Arrays.asList(variants));
        decision.track(decisionModel.getTracker());
        return decision.get();
    }

    /**
     * @see ai.improve.DecisionModel#chooseRandom(List)
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public <T> Decision<T> chooseRandom(List<T> variants) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants can't be null or empty");
        }
        return chooseFrom(variants, ModelUtils.generateRandomGaussians(variants.size()));
    }

    /**
     * @see ai.improve.DecisionModel#random(Object...)
     * @deprecated Remove in 8.0.
     */
    @SafeVarargs
    @Deprecated
    public final <T> T random(T... variants) {
        Decision<T> decision = chooseRandom(Arrays.asList(variants));
        decision.track(decisionModel.getTracker());
        return decision.get();
    }

    /**
     * @see ai.improve.DecisionModel#random(List)
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public <T> T random(List<T> variants) {
        return chooseRandom(variants).get();
    }

    /**
     * @see ai.improve.DecisionModel#chooseMultivariate(Map)
     * @deprecated Remove in 8.0.
     */
    @Deprecated
    public Decision<Map<String, Object>> chooseMultivariate(Map<String, ?> variants) {
        return decide(decisionModel.fullFactorialVariants(variants));
    }
}
