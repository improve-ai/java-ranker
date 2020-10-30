package ai.improve.android;

import java.util.List;
import java.util.Map;

public interface DecisionModel {

    String getModelName();

    /**
     * Takes an array of variants and returns an List of Numbers with the scores.
     */
    List<Number> score(List variants);

    /**
     * Takes an array of variants and context and returns an array of NSNumbers of the scores.
     */
    List<Number> score(List variants, Map context);


}
