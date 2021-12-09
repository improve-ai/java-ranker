package ai.improve.provider;

public interface PersistenceProvider {
    void persistDecisionIdForModel(String modelName, String decisionId);

    String lastDecisionIdForModel(String modelName);

    void addRewardForModel(String modelName, double reward);
}
