package ai.improve.android;

import ai.improve.android.hasher.XXHashAPI;

public class IMPDecisionModel extends BaseIMPDecisionModel {
    public IMPDecisionModel(String modelName) {
        super(modelName, new XXHashAPI());
    }
}
