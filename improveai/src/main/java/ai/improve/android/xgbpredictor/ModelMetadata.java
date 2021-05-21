package ai.improve.android.xgbpredictor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class ModelMetadata {
    private static final String Tag = "ModelMetadata";

    public static final String USER_DEFINED_METADATA = "user_defined_metadata";
    private Map<String, String> storage = new HashMap<>();

    private String modelVersion;

    private String modelName;

    private long modelSeed;

    private List<String> modelFeatureNames;

    public ModelMetadata(ModelReader r) throws IOException, JSONException {
        long num_attrs = r.readLong();
        for (long i = 0; i < num_attrs; ++i) {
            long strlenkey = r.readLong();
            String key = r.readString((int) strlenkey);

            long strlenval = r.readLong();
            String val = r.readString((int) strlenval);

            if("user_defined_metadata".equals(key)) {
                parseMetadata(val);
            }

            storage.put(key, val);
        }
    }

    @Nonnull
    public String getModelName() {
        return modelName;
    }

    @Nonnull
    public String getModelVersion() {
        return modelVersion;
    }

    public long getModelSeed() {
        return modelSeed;
    }

    @Nonnull
    public List<String> getModelFeatureNames() {
        return modelFeatureNames;
    }

    public String getValue(String key) {
        return storage.get(key);
    }

    public String getUserDefinedMetadata() {
        return getValue(USER_DEFINED_METADATA);
    }

    private void parseMetadata(String value) throws JSONException {
        JSONObject root = new JSONObject(value).getJSONObject("json");
        modelName = root.getString("model_name");
        modelVersion = root.getString("version");
        modelSeed = root.getLong("model_seed");

        JSONArray featuresArray = root.getJSONArray("feature_names");
        modelFeatureNames = new ArrayList<>(featuresArray.length());
        for(int i = 0; i < featuresArray.length(); ++i) {
            modelFeatureNames.add(featuresArray.getString(i));
        }
    }
}
