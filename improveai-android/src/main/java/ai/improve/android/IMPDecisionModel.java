package ai.improve.android;

public class IMPDecisionModel extends BaseIMPDecisionModel {
    public IMPDecisionModel(String modelName) {
        super(modelName, new XXHashProviderImp());
    }
}
