package ai.improve.provider;

import java.util.Map;

import ai.improve.DecisionModel;

public interface GivensProvider {
    Map<String, Object> givensForModel(DecisionModel decisionModel, Map<String, Object> givens);
}
