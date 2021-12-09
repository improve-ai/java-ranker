package ai.improve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import static ai.improve.DecisionModelTest.DefaultFailMessage;

import org.junit.jupiter.api.Test;

import ai.improve.DecisionModel;
import ai.improve.log.IMPLog;

public class ModelMapTest {
    public static final String Tag = "ModelMapTest";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testModelInstances() {
        DecisionModel.instances.clear();

        String modelName = "hello";

        assertEquals(0, DecisionModel.instances.size());

        // Create and cache model if not exist
        DecisionModel decisionModel = DecisionModel.instances.get(modelName);
        assertNotNull(decisionModel);
        assertEquals(1, DecisionModel.instances.size());
        assertEquals(decisionModel, DecisionModel.instances.get(modelName));

        DecisionModel.instances.put(modelName, new DecisionModel(modelName));
        assertEquals(modelName, DecisionModel.instances.get(modelName).getModelName());

        // Same object
        assertEquals(DecisionModel.instances.get(modelName), DecisionModel.instances.get(modelName));

        // Overwrite existing model
        assertEquals(1, DecisionModel.instances.size());
        DecisionModel oldModel = DecisionModel.instances.get(modelName);
        DecisionModel.instances.put(modelName, new DecisionModel(modelName));
        DecisionModel newModel = DecisionModel.instances.get(modelName);
        assertNotEquals(oldModel, newModel);
        assertEquals(1, DecisionModel.instances.size());

        // Set as null to remove the existing model
        assertEquals(1, DecisionModel.instances.size());
        DecisionModel.instances.put(modelName, null);
        assertEquals(0, DecisionModel.instances.size());
    }

    @Test
    public void testModelInstances_null_modelName() {
        DecisionModel.instances.clear();
        try {
            DecisionModel.instances.put(null, new DecisionModel("hello"));
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testModelInstances_null_modelName_and_decisionModel() {
        DecisionModel.instances.clear();
        assertEquals(0, DecisionModel.instances.size());
        DecisionModel.instances.put(null, null);
        assertEquals(0, DecisionModel.instances.size());
    }
}
