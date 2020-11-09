package ai.improve.android.spi;

import ai.improve.android.ScoredVariant;
import ai.improve.android.chooser.ImproveChooser;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import ai.improve.android.xgbpredictor.JSONArrayConverter;
import ai.improve.android.DecisionModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultDecisionModel implements DecisionModel {

    private String modelName;

    private ImprovePredictor model;
    private List<Number> lookupTable;
    private int modelSeed;

    private ImproveChooser chooser;

    public static DefaultDecisionModel initWithUrl() {
        return new DefaultDecisionModel("dummy");
    }

    public static DefaultDecisionModel initWithModel(ImprovePredictor model) {
        DefaultDecisionModel instance = new DefaultDecisionModel(model.toString());
        instance.model = model;
        try {
            instance.parseMetadata(model.getModelMetadata().getUserDefinedMetadata());
            instance.init();
        } catch (JSONException ex) {
            //TODO think about this later
            throw new RuntimeException(ex);
        }
        return instance;
    }

    private void parseMetadata(String userDefinedMetadata) throws JSONException {
        JSONObject o = new JSONObject(userDefinedMetadata);
        JSONObject meta = o.getJSONObject("json");
        modelSeed = (int) meta.getLong("model_seed");
        JSONArray array = meta.getJSONArray("table");
        lookupTable = JSONArrayConverter.toList(array);
    }


    DefaultDecisionModel(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    void init() {
        this.chooser = new ImproveChooser(model, lookupTable, modelSeed);
    }

    @Override
    public List<ScoredVariant> score(List<Object> variants) {
        return score(variants, Collections.emptyMap());
    }

    @Override
    public List<ScoredVariant> score(List<Object> variants, Map<String, Object> context) {
        return chooser.score(variants, context);
    }
}
