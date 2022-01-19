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

    public DecisionContext(DecisionModel decisionModel, Map givens) {
        this.decisionModel = decisionModel;
        this.givens = givens;
    }

    /**
     * @see ai.improve.DecisionModel#chooseFrom(List)
     */
    public Decision chooseFrom(List variants) {
        if(variants == null || variants.size() <= 0) {
            throw new IllegalArgumentException("variants to choose from can't be null or empty");
        }

        Map allGivens = decisionModel.combinedGivens(givens);

        List scores = decisionModel.scoreInternal(variants, allGivens);

        Object best = ModelUtils.topScoringVariant(variants, scores);

        Decision decision = new Decision(decisionModel);
        decision.variants = variants;
        decision.best = best;
        decision.givens = allGivens;
        decision.scores = scores;

        return decision;
    }

    /**
     * @see ai.improve.DecisionModel#chooseMultiVariate(Map)
     */
    public Decision chooseMultiVariate(Map<String, ?> variants) {
        if(variants == null || variants.size() <= 0) {
            // Let it pass here.
            // Exception would be thrown later when calling get()
            return chooseFrom(null);
        }

        List allKeys = new ArrayList();

        List<List> categories = new ArrayList();
        for(Map.Entry<String, ?> entry : variants.entrySet()) {
            if(entry.getValue() instanceof List) {
                categories.add((List)entry.getValue());
            } else {
                categories.add(Arrays.asList(entry.getValue()));
            }
            allKeys.add(entry.getKey());
        }

        List<Map> combinations = new ArrayList();
        for(int i = 0; i < categories.size(); ++i) {
            List category = categories.get(i);
            List<Map> newCombinations = new ArrayList();
            for(int m = 0; m < category.size(); ++m) {
                if(combinations.size() == 0) {
                    Map newVariant = new HashMap();
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

    /**
     * @param variants Variants can be any JSON encodeable data structure of arbitrary complexity,
     *                 including nested dictionaries,
     *  arrays, strings, numbers, nulls, and booleans.
     * @throws IllegalArgumentException Thrown if variants is nil or empty.
     * @return scores of the variants
     */
    public <T> List<Double> score(List<T> variants) {
        Map allGivens = decisionModel.combinedGivens(givens);
        return decisionModel.scoreInternal(variants, allGivens);
    }

    /**
     * @see ai.improve.DecisionModel#which(Object...) 
     */
    public Object which(Object... variants) {
        if(variants == null || variants.length <= 0) {
            throw new IllegalArgumentException("should at least provide one variant.");
        } else if(variants.length == 1) {
            if(variants[0] instanceof List) {
                return chooseFrom((List)variants[0]).get();
            } else if(variants[0] instanceof Map) {
                return chooseMultiVariate((Map)variants[0]).get();
            }
            throw new IllegalArgumentException("If only one argument, it must be a List or Map");
        } else {
            return chooseFrom(Arrays.asList(variants)).get();
        }
    }
}
