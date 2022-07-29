package ai.improve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.util.ModelUtils;

public class DecisionContext {

    private DecisionModel decisionModel;

    private Map givens;

    protected DecisionContext(DecisionModel decisionModel, Map givens) {
        this.decisionModel = decisionModel;
        this.givens = givens;
    }

    /**
     * @see ai.improve.DecisionModel#chooseFrom(List)
     */
    public <T> Decision<T> chooseFrom(List<T> variants) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants to choose from can't be null or empty");
        }

        Map allGivens = decisionModel.combinedGivens(givens);
        List scores = decisionModel.scoreInternal(variants, allGivens);
        T best = (T) ModelUtils.topScoringVariant(variants, scores);

        Decision<T> decision = new Decision(decisionModel);
        decision.variants = variants;
        decision.best = best;
        decision.givens = allGivens;
        decision.scores = scores;

        return decision;
    }

    /**
     * @see ai.improve.DecisionModel#chooseFrom(List, List)
     */
    public <T> Decision<T> chooseFrom(List<T> variants, List<Double> scores) {
        if(variants == null || scores == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants and scores can't be null or empty");
        }
        if(variants.size() != scores.size()) {
            throw new IllegalArgumentException("variants.size(" +
                    variants.size() + ") not equal to scores.size(" +
                    scores.size() + ")");
        }

        Map allGivens = decisionModel.combinedGivens(givens);

        Object best = ModelUtils.topScoringVariant(variants, scores);
        Decision decision = new Decision(decisionModel);
        decision.variants = variants;
        decision.best = best;
        decision.givens = allGivens;
        decision.scores = scores;
        return decision;
    }

    /**
     * @see ai.improve.DecisionModel#chooseFrom(List)
     */
    public <T> Decision<T> chooseFirst(List<T> variants) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants can't be null or empty");
        }
        return chooseFrom(variants, ModelUtils.generateDescendingGaussians(variants.size()));
    }

    public <T> T first(List<T> variants) {
        return chooseFirst(variants).get();
    }

    /**
     * @see ai.improve.DecisionModel#first(Object...)
     */
    public <T> T first(T... variants) {
        return chooseFirst(Arrays.asList(variants)).get();
    }

    /**
     * @see ai.improve.DecisionModel#chooseRandom(List)
     */
    public <T> Decision<T> chooseRandom(List<T> variants) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants can't be null or empty");
        }
        return chooseFrom(variants, ModelUtils.generateRandomGaussians(variants.size()));
    }

    /**
     * @see ai.improve.DecisionModel#random(Object...)
     */
    public <T> T random(T... variants) {
        return chooseRandom(Arrays.asList(variants)).get();
    }

    public <T> T random(List<T> variants) {
        return chooseRandom(variants).get();
    }

    /**
     * @see ai.improve.DecisionModel#chooseMultivariate(Map)
     */
    public Decision<Map<String, ?>> chooseMultivariate(Map<String, ?> variants) {
        if(variants == null || variants.size() <= 0) {
            return chooseFrom(null);
        }

        List<String> allKeys = new ArrayList();

        List<List> categories = new ArrayList();
        for(Map.Entry<String, ?> entry : variants.entrySet()) {
            if(entry.getValue() instanceof List) {
                if(((List)entry.getValue()).size() > 0) {
                    categories.add((List) entry.getValue());
                    allKeys.add(entry.getKey());
                }
            } else {
                categories.add(Arrays.asList(entry.getValue()));
                allKeys.add(entry.getKey());
            }
        }
        if(categories.size() <= 0) {
            throw new IllegalArgumentException("valueMap values are all empty list!");
        }

        List<Map<String, ?>> combinations = new ArrayList();
        for(int i = 0; i < categories.size(); ++i) {
            List category = categories.get(i);
            List<Map<String, ?>> newCombinations = new ArrayList();
            for(int m = 0; m < category.size(); ++m) {
                if(combinations.size() == 0) {
                    Map<String, Object> newVariant = new HashMap();
                    newVariant.put(allKeys.get(i), category.get(m));
                    newCombinations.add(newVariant);
                } else {
                    for(int n = 0; n < combinations.size(); ++n) {
                        Map newVariant = new HashMap(combinations.get(n));
                        newVariant.put(allKeys.get(i), category.get(m));
                        newCombinations.add(newVariant);
                    }
                }
            }
            combinations = newCombinations;
        }

        return chooseFrom(combinations);
    }

    public Map<String, ?> optimize(Map<String, ?> variants) {
        return chooseMultivariate(variants).get();
    }

    /**
     * @see ai.improve.DecisionModel#score(List)
     */
    public <T> List<Double> score(List<T> variants) {
        Map allGivens = decisionModel.combinedGivens(givens);
        return decisionModel.scoreInternal(variants, allGivens);
    }

    /**
     * @see ai.improve.DecisionModel#which(Object...) 
     */
    public <T> T which(T... variants) {
        if(variants == null || variants.length <= 0) {
            throw new IllegalArgumentException("should at least provide one variant.");
        }
        return chooseFrom(Arrays.asList(variants)).get();
    }

    public <T> T which(List<T> variants) {
        return chooseFrom(variants).get();
    }
}
