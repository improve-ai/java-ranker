package ai.improve.android.xgbpredictor;

import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModelMetadata {

    public static final String USER_DEFINED_METADATA = "user_defined_metadata";
    private Map<String, String> storage = new HashMap<>();

    public ModelMetadata(ModelReader r) throws IOException {
        long num_attrs = r.readLong();
        for (long i = 0; i < num_attrs; ++i) {
            long strlenkey = r.readLong();
            String key = r.readString((int) strlenkey);

            long strlenval = r.readLong();
            String val = r.readString((int) strlenval);

            storage.put(key, val);
        }
    }

    public String getValue(String key) {
        return storage.get(key);
    }

    public String getUserDefinedMetadata() {
        return getValue(USER_DEFINED_METADATA);
    }
}
