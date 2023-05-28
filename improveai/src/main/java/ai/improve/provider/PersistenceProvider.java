package ai.improve.provider;

public interface PersistenceProvider {
    void write(String key, String value);
}
