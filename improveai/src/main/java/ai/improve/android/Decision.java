package ai.improve.android;

import java.util.List;

public interface Decision {

    List<Double> scores(List<Object> variants);

    List<Object> ranked(List<Object> variants);

    List<ScoredVariant> scored(List<Object> variants);

    Object best(List<Object> variants);

    List<Object> topRunnersUp(List<Object> variants);


}
