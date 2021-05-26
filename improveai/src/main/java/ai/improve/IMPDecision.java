package ai.improve;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class IMPDecision {
    public static final String Tag = "IMPDecision";

    private BaseIMPDecisionModel model;

    private List<Object> variants;

    private Map<String, Object> givens;

    private boolean chosen;

    private Object best;

    public IMPDecision(BaseIMPDecisionModel model) {
        this.model = model;
    }

    public IMPDecision chooseFrom(List<Object> variants) {
        if(chosen) {
            IMPLog.e(Tag, "variant already chosen, ignoring variants");
        } else {
            this.variants = variants;
        }
        return this;
    }

    public IMPDecision given(Map<String, Object> givens) {
        if(chosen) {
            IMPLog.e(Tag, "variant already chosen, ignoring givens");
        } else {
            this.givens = givens;
        }
        return this;
    }

    public synchronized Object get() {
        if(chosen) {
            return best;
        }

        List<Double> scores = model.score(variants, givens);

        if(variants != null && variants.size() > 0) {
            BaseIMPDecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                if(IMPUtils.shouldtrackRunnersUp(variants.size(), tracker.getMaxRunnersUp())) {
                    // the more variants there are, the less frequently this is called
                    List<Object> rankedVariants = BaseIMPDecisionModel.rank(variants, scores);
                    best = rankedVariants.get(0);
                    track(tracker, best, variants, givens, model.getModelName(), true);
                } else {
                    // faster and more common path, avoids array sort
                    best = IMPUtils.topScoringVariant(variants, scores);
                    track(tracker, best, variants, givens, model.getModelName(), false);
                }
            } else {
                best = IMPUtils.topScoringVariant(variants, scores);
            }
        } else {
            // Unit test that "variant": null JSON is tracked on null or empty variants.
            // "count" field should be 1
            best = null;
            BaseIMPDecisionTracker tracker = model.getTracker();
            if(tracker != null) {
                track(tracker, best, variants, givens, model.getModelName(), false);
            }
        }

        chosen = true;

        return best;
    }

    private void track(BaseIMPDecisionTracker tracker, Object bestVariant, List<Object> variants,
                       Map<String, Object> givens, String modelName,
                       boolean variantsRankedAndTrackRunnersUp) {
        Method method = getDeclaredMethod(tracker, "track", Object.class, List.class,
                Map.class, String.class, boolean.class);
        method.setAccessible(true);
        try {
            method.invoke(tracker, bestVariant, variants, givens, modelName, variantsRankedAndTrackRunnersUp);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Method getDeclaredMethod(Object object, String methodName, Class<?> ... parameterTypes){
        Method method;
        for(Class<?> clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes) ;
                return method ;
            } catch (Exception e) {
            }
        }
        return null;
    }
}
