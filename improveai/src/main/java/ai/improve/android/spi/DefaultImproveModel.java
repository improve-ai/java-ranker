package ai.improve.android.spi;

import biz.k11i.xgboost.Predictor;
import ai.improve.android.ImproveModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultImproveModel implements ImproveModel {

    private String modelName;

    private Predictor model;

    public static DefaultImproveModel initWithUrl() {
        return new DefaultImproveModel("dummy");
    }

    public DefaultImproveModel(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public Object choose(List variants) {
        // Stubbed out method
        return variants.get(0);
    }

    @Override
    public Object choose(List variants, Map context) {
        return variants.get(0);
    }

    @Override
    public List sort(List variants) {
        return variants;
    }

    @Override
    public List sort(List variants, Map context) {
        return variants;
    }

    @Override
    public List<Number> score(List variants) {
        return score(variants, Collections.EMPTY_MAP);
    }

    @Override
    public List<Number> score(List variants, Map context) {
        return Collections.nCopies(variants.size(), 0);
    }
}
