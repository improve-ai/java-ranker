package ai.improve.android.spi;

import ai.improve.android.Decision;
import ai.improve.android.ScoredVariant;

import java.util.Collections;
import java.util.List;

public class DecisionMaker implements Decision {


    @Override
    public List<Double> scores(List<Object> variants) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<Object> ranked(List<Object> variants) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<ScoredVariant> scored(List<Object> variants) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Object best(List<Object> variants) {
        if(variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.get(0);
    }

    @Override
    public List<Object> topRunnersUp(List<Object> variants) {
        return variants;
    }
}
