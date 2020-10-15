package ai.improve.android.spi;

import ai.improve.android.xgbpredictor.ImprovePredictor;
import ai.improve.android.xgbpredictor.JSONArrayConverter;
import biz.k11i.xgboost.Predictor;
import ai.improve.android.ImproveModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultImproveModel implements ImproveModel {

    private String modelName;

    private ImprovePredictor model;
    private List<Number> lookupTable;
    private long modelSeed;

    public static DefaultImproveModel initWithUrl() {
        return new DefaultImproveModel("dummy");
    }

    public static DefaultImproveModel initWithModel(ImprovePredictor model) {
        DefaultImproveModel instance = new DefaultImproveModel(model.toString());
        instance.model = model;
        try {
            instance.parseMetadata(model.getModelMetadata().getUserDefinedMetadata());
        } catch(JSONException ex) {
            //TODO think about this later
            throw new RuntimeException(ex);
        }
        return instance;
    }

    private void parseMetadata(String userDefinedMetadata) throws JSONException {
        JSONObject o = new JSONObject(userDefinedMetadata);
        JSONObject meta = o.getJSONObject("json");
        modelSeed = meta.getLong("model_seed");
        JSONArray array = meta.getJSONArray("table");
        lookupTable = JSONArrayConverter.toList(array);
    }


    DefaultImproveModel(String modelName) {
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
