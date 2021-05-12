package ai.improve.android;

import java.io.IOException;

import biz.k11i.xgboost.util.ModelReader;

public class IMPModelMetadata {
    public IMPModelMetadata(ModelReader r) throws IOException {
        long num_attrs = r.readLong();
        for (long i = 0; i < num_attrs; ++i) {
            long strlenkey = r.readLong();
            String key = r.readString((int) strlenkey);

            long strlenval = r.readLong();
            String val = r.readString((int) strlenval);

//            storage.put(key, val);
        }
    }
}
