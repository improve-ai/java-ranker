package ai.improve.util;

import java.util.HashMap;
import java.util.Map;

import ai.improve.DecisionModel;

public class ModelMap {
    private Map<String, DecisionModel> models = new HashMap<>();

    /**
     * Get the DecisionModel instance with the specified modelName. If the model does not exist,
     * create and cache it.
     * @param modelName Must be a valid modelName. Please check class DecisionModel for what's a
     *                  valid modelName.
     * @return A nonnull DecisionModel instance with the specified modelName.
     * */
    public synchronized DecisionModel get(String modelName) {
        if(models.containsKey(modelName)) {
            return models.get(modelName);
        }
        DecisionModel decisionModel = new DecisionModel(modelName);
        models.put(modelName, decisionModel);
        return decisionModel;
    }

    /**
     * @param modelName Must be a valid modelName. Please check class DecisionModel for what's a
     *                  valid modelName.
     * @param decisionModel If null, the cached decisionModel with the specified modelName would be
     *                      removed.
     * @throws IllegalArgumentException Thrown if decisionModel is not null and modelName is not
     * equal to decisionModel.modelName.
     * */
    public synchronized void put(String modelName, DecisionModel decisionModel) {
        if(decisionModel != null && !decisionModel.getModelName().equals(modelName)) {
            String reason = String.format("modelName(%s) must be equal to decisionModel.modelName(%s)",
                    modelName, decisionModel.getModelName());
            throw new IllegalArgumentException(reason);
        }
        if(decisionModel == null) {
            models.remove(modelName);
        } else {
            models.put(modelName, decisionModel);
        }
    }

    public void clear() {
        models.clear();
    }

    public int size() {
        return models.size();
    }
}
