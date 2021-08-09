package ai.improve.xgbpredictor;

import biz.k11i.xgboost.util.ModelReader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelMetadata {
    private static final String Tag = "ModelMetadata";

    public static final String USER_DEFINED_METADATA = "user_defined_metadata";
    private Map<String, String> storage = new HashMap<>();

    private String modelVersion;

    private String modelName;

    private long modelSeed;

    private List<String> modelFeatureNames;

    public ModelMetadata(ModelReader r) throws IOException {
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

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public long getModelSeed() {
        return modelSeed;
    }

    public List<String> getModelFeatureNames() {
        return modelFeatureNames;
    }

    public String getValue(String key) {
        return storage.get(key);
    }

    public String getUserDefinedMetadata() {
        return getValue(USER_DEFINED_METADATA);
    }

    private void parseMetadata(String value) {
        JsonObject root = JsonParser.parseString(value).getAsJsonObject();
        modelName = root.get("model").getAsString();
        modelVersion = root.get("version").getAsString();
        modelSeed = root.get("model_seed").getAsLong();

        JsonArray featuresArray = root.get("feature_names").getAsJsonArray();
        modelFeatureNames = new ArrayList<>(featuresArray.size());
        for(int i = 0; i < featuresArray.size(); ++i) {
            modelFeatureNames.add(featuresArray.get(i).getAsString());
        }

    }
}
