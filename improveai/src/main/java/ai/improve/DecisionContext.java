package ai.improve;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.util.ModelUtils;

public class DecisionContext {

    private final DecisionModel decisionModel;

    private Map<String, ?> givens = null;

    private Gson gson = new Gson();

    protected DecisionContext(DecisionModel decisionModel, Map<String, ?> givens) {
        this.decisionModel = decisionModel;
        if(givens != null) {
            this.givens = new HashMap<>(givens);
        }
    }

    /**
     * @see ai.improve.DecisionModel#score(List)
     */
    public <T> List<Double> score(List<T> variants) {
        Map<String, ?> allGivens = getAllGivens();
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

        Map<String, ?> allGivens = getAllGivens();
        List<Double> scores = null;
        List<T> rankedVariants;
        if(ordered) {
            rankedVariants = new ArrayList<>(variants);
        } else {
            if(decisionModel.isLoaded()) {
                scores = decisionModel.scoreInternal(variants, allGivens);
                rankedVariants = DecisionModel.rank(variants, scores);
            } else {
                rankedVariants = new ArrayList<>(variants);
            }
        }
        return new Decision<>(decisionModel, rankedVariants, allGivens, scores);
    }

    /**
     * @see ai.improve.DecisionModel#decide(List, List)
     */
    public <T> Decision<T> decide(List<T> variants, List<Double> scores) {
        Map<String, ?> allGivens = getAllGivens();
        List<T> rankedVariants = DecisionModel.rank(variants, scores);
        return new Decision<>(decisionModel, rankedVariants, allGivens, scores);
    }

    /**
     * @see ai.improve.DecisionModel#which(Object[])
     */
    @SafeVarargs
    public final <T> T which(T... variants) {
        if(variants == null || variants.length <= 0) {
            throw new IllegalArgumentException("should at least provide one variant.");
        }
        return whichFrom(Arrays.asList(variants));
    }

    /**
     * @see ai.improve.DecisionModel#which(Object[])
     */
    public <T> T whichFrom(List<T> variants) {
        Decision<T> decision = decide(variants);

        DecisionTracker tracker = decisionModel.getTracker();
        if(tracker != null) {
            tracker.track(decision.ranked, decision.givens, decisionModel.getModelName());
        }

        return decision.best;
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
        return whichFrom(DecisionModel.fullFactorialVariants(variantMap));
    }

    /** @see ai.improve.DecisionModel#optimize(Map, Class)  */
    public <T> T optimize(Map<String, ?> variantMap, Class<T> classOfT) {
        Map<String, ?> variant = whichFrom(DecisionModel.fullFactorialVariants(variantMap));
        return gson.fromJson(gson.toJsonTree(variant), classOfT);
    }

    /** @hidden */
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

        Map<String, ?> allGivens = getAllGivens();

        return tracker.track(variant, allGivens, runnersUp, sample, samplePoolSize, decisionModel.getModelName());
    }

    private Map<String, ?> getAllGivens() {
        Map<String, ?> allGivens = this.givens;
        GivensProvider givensProvider = decisionModel.getGivensProvider();
        if(givensProvider != null) {
            allGivens = givensProvider.givensForModel(decisionModel, this.givens);
        }
        return allGivens;
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
        return decide(variants, true);
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
        return chooseFirst(Arrays.asList(variants)).get();
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
        return decide(variants, ModelUtils.generateRandomGaussians(variants.size()));
    }

    /**
     * @see ai.improve.DecisionModel#random(Object...)
     * @deprecated Remove in 8.0.
     */
    @SafeVarargs
    @Deprecated
    public final <T> T random(T... variants) {
        return chooseRandom(Arrays.asList(variants)).get();
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
        return decide(DecisionModel.fullFactorialVariants(variants));
    }
}
