package ai.improve.android;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.improve.android.hasher.XXFeatureEncoder;
import ai.improve.android.spi.ModelDownloader;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.FVec;

public class IMPDecisionModel {
    public static final String Tag = "IMPDecisionModel";

    private String modelName;

    private IMPDecisionTracker tracker;

    private ImprovePredictor predictor;

    private XXFeatureEncoder featureEncoder;

    public static IMPDecisionModel loadFromAsset(Context context, String filename) {
        IMPDecisionModel model = new IMPDecisionModel("");
        try {
            ImprovePredictor predictor = ModelDownloader.fromAsset(context, filename);
            model.setModel(predictor);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return model;
    }

    public IMPDecisionModel(String modelName) {
        this.modelName = modelName;
    }

    public void setModel(ImprovePredictor predictor) {
        this.predictor = predictor;

        if(!TextUtils.isEmpty(modelName) && !modelName.equals(predictor.getModelMetadata().getModelName())) {
            IMPLog.w(Tag, "Model names don't match: Current model name [" + modelName
                    + "], new model Name [" + predictor.getModelMetadata().getModelName() +"]");
        }
        this.modelName = predictor.getModelMetadata().getModelName();

        featureEncoder = new XXFeatureEncoder(predictor.getModelMetadata().getModelSeed(),
                predictor.getModelMetadata().getModelFeatureNames());
    }

    public IMPDecision chooseFrom(List<Object> variants) {
        return new IMPDecision(this).chooseFrom(variants);
    }

    public IMPDecision given(Map<String, Object> givens) {
        return new IMPDecision(this).given(givens);
    }

    public List<Float> score(List<Object> variants) {
        return this.score(variants, null);
    }

    public List<Float> score(List<Object> variants, Map<String, Object> givens) {
        List<Float> result = new ArrayList<>();

        if(variants == null || variants.size() <= 0) {
            return result;
        }

        List<FVec> encodedFeatures = featureEncoder.encodeVariants(variants, givens);
        for (FVec fvec : encodedFeatures) {
            result.add(predictor.predictSingle(fvec));
        }

        return result;
    }

    public static interface IMPDecisionModelLoadListener {
        void onFinishLoadingModel(IMPDecisionModel model, int error, String errMsg);
    }
}
