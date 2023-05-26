package ai.improve.provider;

public interface PersistenceProvider {
    void persistDecisionIdForModel(String modelName, String decisionId);

    String lastDecisionIdForModel(String modelName);

    void write(String key, String value);
}
