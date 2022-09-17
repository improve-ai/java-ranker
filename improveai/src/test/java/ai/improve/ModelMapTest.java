package ai.improve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import static ai.improve.DecisionModelTest.DefaultFailMessage;

import org.junit.jupiter.api.Test;

import ai.improve.log.IMPLog;

public class ModelMapTest {
    public static final String Tag = "ModelMapTest";

    static {
        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);
    }

    @Test
    public void testModelInstances() {
        DecisionModel.clearInstances();

        String modelName = "hello";

        assertEquals(0, DecisionModel.sizeOfInstances());

        // Create and cache model if not exist
        DecisionModel decisionModel = DecisionModel.get(modelName);
        assertNotNull(decisionModel);
        assertEquals(1, DecisionModel.sizeOfInstances());
        assertEquals(decisionModel, DecisionModel.get(modelName));

        DecisionModel.put(modelName, new DecisionModel(modelName));
        assertEquals(modelName, DecisionModel.get(modelName).getModelName());

        // Same object
        assertEquals(DecisionModel.get(modelName), DecisionModel.get(modelName));

        // Overwrite existing model
        assertEquals(1, DecisionModel.sizeOfInstances());
        DecisionModel oldModel = DecisionModel.get(modelName);
        DecisionModel.put(modelName, new DecisionModel(modelName));
        DecisionModel newModel = DecisionModel.get(modelName);
        assertNotEquals(oldModel, newModel);
        assertEquals(1, DecisionModel.sizeOfInstances());

        // Set as null to remove the existing model
        assertEquals(1, DecisionModel.sizeOfInstances());
        DecisionModel.put(modelName, null);
        assertEquals(0, DecisionModel.sizeOfInstances());
    }

    @Test
    public void testModelInstances_null_modelName() {
        DecisionModel.clearInstances();
        try {
            DecisionModel.put(null, new DecisionModel("hello"));
        } catch (IllegalArgumentException e) {
            return ;
        }
        fail(DefaultFailMessage);
    }

    @Test
    public void testModelInstances_null_modelName_and_decisionModel() {
        DecisionModel.clearInstances();
        assertEquals(0, DecisionModel.sizeOfInstances());
        DecisionModel.put(null, null);
        assertEquals(0, DecisionModel.sizeOfInstances());
    }
}
