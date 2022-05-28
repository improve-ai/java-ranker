package ai.improve.xgbpredictor;

import ai.improve.constants.BuildProperties;
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

    public static final String IMPROVE_VERSION_KEY = "ai.improve.version";

    private Map<String, String> storage = new HashMap<>();

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

    private void parseMetadata(String value) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(value).getAsJsonObject().getAsJsonObject("json");
            if(root.has(IMPROVE_VERSION_KEY)) {
                String modelVersion = root.get(IMPROVE_VERSION_KEY).getAsString();
                if(!canParseModel(modelVersion, BuildProperties.getSDKVersion())) {
                    throw new IOException("Major version don't match. ImproveAI SDK version(" + BuildProperties.getSDKVersion()+") " +
                            "can't load the model of version("+ modelVersion + ").");
                }
            }
            modelName = root.get("model_name").getAsString();
            modelSeed = root.get("model_seed").getAsLong();

            JsonArray featuresArray = root.get("feature_names").getAsJsonArray();
            modelFeatureNames = new ArrayList<>(featuresArray.size());
            for (int i = 0; i < featuresArray.size(); ++i) {
                modelFeatureNames.add(featuresArray.get(i).getAsString());
            }
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse the model metadata. Looks like the model being loaded is invalid.");
        }
    }

    /**
     * Check if the SDK can parse the model.
     * @return Returns true, if {@value IMPROVE_VERSION_KEY} property is null;
     * Returns true if the @{value IMPROVE_VERSION_KEY} property is not null and its major version
     * matches the major version of the SDK; otherwise, return false.
     */
    public static boolean canParseModel(String modelVersion, String sdkVersion) {
        if(modelVersion == null) {
            return true;
        }
        String modelMajorVersion = modelVersion.split("\\.")[0];
        String sdkMajorVersion = sdkVersion.split("\\.")[0];
        return modelMajorVersion.equals(sdkMajorVersion);
    }
}
