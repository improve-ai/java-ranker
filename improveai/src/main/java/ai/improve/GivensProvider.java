package ai.improve;

import java.util.Map;

public interface GivensProvider {
    Map<String, Object> givensForModel(DecisionModel decisionModel, Map<String, ?> givens);
}
