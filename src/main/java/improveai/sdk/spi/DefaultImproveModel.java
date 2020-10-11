package improveai.sdk.spi;

import biz.k11i.xgboost.Predictor;
import improveai.sdk.ImproveModel;

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
}
