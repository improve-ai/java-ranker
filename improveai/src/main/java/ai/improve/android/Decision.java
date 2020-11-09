package ai.improve.android;

import java.util.List;
import java.util.Map;

public interface Decision {

    List<Object> getVariants();

    Map<String, Object> getContext();

    boolean isTrackRunnersUp();

    List<? extends Number> scores();

    List<Object> ranked();

    List<ScoredVariant> scored();

    Object best();

    List<Object> topRunnersUp();


    Object getModelName();
}
