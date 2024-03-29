package ai.improve.xgbpredictor;

import biz.k11i.xgboost.config.PredictorConfiguration;
import biz.k11i.xgboost.gbm.GradBooster;
import biz.k11i.xgboost.learner.ObjFunction;
import biz.k11i.xgboost.spark.SparkModelParam;
import biz.k11i.xgboost.util.FVec;
import biz.k11i.xgboost.util.ModelReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Predicts using the Xgboost model.
 */
public class ImprovePredictor implements Serializable {
    private ModelParam mparam;
    private SparkModelParam sparkModelParam;
    private String name_obj;
    private String name_gbm;
    private ObjFunction obj;
    private GradBooster gbm;
    private ModelMetadata modelMetadata;

    private float base_score;

    public ImprovePredictor(InputStream in) throws IOException {
        this(in, null);
    }

    /**
     * Instantiates with the Xgboost model
     *
     * @param in input stream
     * @param configuration configuration
     * @throws IOException If an I/O error occurs
     */
    public ImprovePredictor(InputStream in, PredictorConfiguration configuration) throws IOException {
        if (configuration == null) {
            configuration = PredictorConfiguration.DEFAULT;
        }

        ModelReader reader = new ModelReader(in);

        readParam(reader);
        initObjFunction(configuration);
        initObjGbm();

        gbm.loadModel(configuration, reader, mparam.saved_with_pbuffer != 0);

        if (mparam.major_version >= 1) {
            base_score = obj.probToMargin(mparam.base_score);
        } else {
            base_score = mparam.base_score;
        }

        modelMetadata = new ModelMetadata(reader);
    }


    void readParam(ModelReader reader) throws IOException {
        byte[] first4Bytes = reader.readByteArray(4);
        byte[] next4Bytes = reader.readByteArray(4);

        float base_score;
        int num_feature;

        if (first4Bytes[0] == 0x62 &&
                first4Bytes[1] == 0x69 &&
                first4Bytes[2] == 0x6e &&
                first4Bytes[3] == 0x66) {

            // Old model file format has a signature "binf" (62 69 6e 66)
            base_score = reader.asFloat(next4Bytes);
            num_feature = reader.readUnsignedInt();

        } else if (first4Bytes[0] == 0x00 &&
                first4Bytes[1] == 0x05 &&
                first4Bytes[2] == 0x5f) {

            // Model generated by xgboost4j-spark?
            String modelType = null;
            if (first4Bytes[3] == 0x63 &&
                    next4Bytes[0] == 0x6c &&
                    next4Bytes[1] == 0x73 &&
                    next4Bytes[2] == 0x5f) {
                // classification model
                modelType = SparkModelParam.MODEL_TYPE_CLS;

            } else if (first4Bytes[3] == 0x72 &&
                    next4Bytes[0] == 0x65 &&
                    next4Bytes[1] == 0x67 &&
                    next4Bytes[2] == 0x5f) {
                // regression model
                modelType = SparkModelParam.MODEL_TYPE_REG;
            }

            if (modelType != null) {
                int len = (next4Bytes[3] << 8) + (reader.readByteAsInt());
                String featuresCol = reader.readUTF(len);

                this.sparkModelParam = new SparkModelParam(modelType, featuresCol, reader);

                base_score = reader.readFloat();
                num_feature = reader.readUnsignedInt();

            } else {
                base_score = reader.asFloat(first4Bytes);
                num_feature = reader.asUnsignedInt(next4Bytes);
            }

        } else {
            base_score = reader.asFloat(first4Bytes);
            num_feature = reader.asUnsignedInt(next4Bytes);
        }

        mparam = new ModelParam(base_score, num_feature, reader);

        name_obj = reader.readString();
        name_gbm = reader.readString();
    }

    void initObjFunction(PredictorConfiguration configuration) {
        obj = configuration.getObjFunction();

        if (obj == null) {
            obj = ObjFunction.fromName(name_obj);
        }
    }

    void initObjGbm() {
        obj = ObjFunction.fromName(name_obj);
        gbm = GradBooster.Factory.createGradBooster(name_gbm);
        gbm.setNumClass(mparam.num_class);
        gbm.setNumFeature(mparam.num_feature);
    }

    /**
     * Generates predictions for given feature vector.
     *
     * @param feat feature vector
     * @return prediction values
     */
    public float[] predict(FVec feat) {
        return predict(feat, false);
    }

    /**
     * Generates predictions for given feature vector.
     *
     * @param feat          feature vector
     * @param output_margin whether to only predict margin value instead of transformed prediction
     * @return prediction values
     */
    public float[] predict(FVec feat, boolean output_margin) {
        return predict(feat, output_margin, 0);
    }

    /**
     * Generates predictions for given feature vector.
     *
     * @param feat          feature vector
     * @param base_margin   predict with base margin for each prediction
     * @return prediction values
     */
    public float[] predict(FVec feat, float base_margin) {
        return predict(feat, base_margin, 0);
    }

    /**
     * Generates predictions for given feature vector.
     *
     * @param feat          feature vector
     * @param base_margin   predict with base margin for each prediction
     * @param ntree_limit   limit the number of trees used in prediction
     * @return prediction values
     */
    public float[] predict(FVec feat, float base_margin, int ntree_limit) {
        float[] preds = predictRaw(feat, base_margin, ntree_limit);
        preds = obj.predTransform(preds);
        return preds;
    }

    /**
     * Generates predictions for given feature vector.
     *
     * @param feat          feature vector
     * @param output_margin whether to only predict margin value instead of transformed prediction
     * @param ntree_limit   limit the number of trees used in prediction
     * @return prediction values
     */
    public float[] predict(FVec feat, boolean output_margin, int ntree_limit) {
        float[] preds = predictRaw(feat, base_score, ntree_limit);
        if (! output_margin) {
            preds = obj.predTransform(preds);
        }
        return preds;
    }

    float[] predictRaw(FVec feat, float base_score, int ntree_limit) {
        float[] preds = gbm.predict(feat, ntree_limit);
        for (int i = 0; i < preds.length; i++) {
            preds[i] += base_score;
        }
        return preds;
    }

    /**
     * Generates a prediction for given feature vector.
     * <p>
     * This method only works when the model outputs single value.
     * </p>
     *
     * @param feat feature vector
     * @return prediction value
     */
    public float predictSingle(FVec feat) {
        return predictSingle(feat, false);
    }

    /**
     * Generates a prediction for given feature vector.
     * <p>
     * This method only works when the model outputs single value.
     * </p>
     *
     * @param feat          feature vector
     * @param output_margin whether to only predict margin value instead of transformed prediction
     * @return prediction value
     */
    public float predictSingle(FVec feat, boolean output_margin) {
        return predictSingle(feat, output_margin, 0);
    }

    /**
     * Generates a prediction for given feature vector.
     * <p>
     * This method only works when the model outputs single value.
     * </p>
     *
     * @param feat          feature vector
     * @param output_margin whether to only predict margin value instead of transformed prediction
     * @param ntree_limit   limit the number of trees used in prediction
     * @return prediction value
     */
    public float predictSingle(FVec feat, boolean output_margin, int ntree_limit) {
        float pred = predictSingleRaw(feat, ntree_limit);
        if (!output_margin) {
            pred = obj.predTransform(pred);
        }
        return pred;
    }

    float predictSingleRaw(FVec feat, int ntree_limit) {
        return gbm.predictSingle(feat, ntree_limit) + base_score;
    }

    /**
     * Predicts leaf index of each tree.
     *
     * @param feat feature vector
     * @return leaf indexes
     */
    public int[] predictLeaf(FVec feat) {
        return predictLeaf(feat, 0);
    }

    /**
     * Predicts leaf index of each tree.
     *
     * @param feat        feature vector
     * @param ntree_limit limit, 0 for all
     * @return leaf indexes
     */
    public int[] predictLeaf(FVec feat, int ntree_limit) {
        return gbm.predictLeaf(feat, ntree_limit);
    }

    /**
     * Predicts path to leaf of each tree.
     *
     * @param feat        feature vector
     * @return leaf paths
     */
    public String[] predictLeafPath(FVec feat) {
        return predictLeafPath(feat, 0);
    }

    /**
     * Predicts path to leaf of each tree.
     *
     * @param feat        feature vector
     * @param ntree_limit limit, 0 for all
     * @return leaf paths
     */
    public String[] predictLeafPath(FVec feat, int ntree_limit) {
        return gbm.predictLeafPath(feat, ntree_limit);
    }

    public SparkModelParam getSparkModelParam() {
        return sparkModelParam;
    }

    public ModelMetadata getModelMetadata() {
        return modelMetadata;
    }

    /**
     * Returns number of class.
     *
     * @return number of class
     */
    public int getNumClass() {
        return mparam.num_class;
    }

    /**
     * Parameters.
     */
    static class ModelParam implements Serializable {
        /* \brief global bias */
        final float base_score;
        /* \brief number of features  */
        final /* unsigned */ int num_feature;
        /* \brief number of class, if it is multi-class classification  */
        final int num_class;
        /*! \brief whether the model itself is saved with pbuffer */
        final int saved_with_pbuffer;
        /*! \brief Model contain eval metrics */
        private final int contain_eval_metrics;
        /*! \brief the version of XGBoost. */
        private final int major_version;
        private final int minor_version;
        /*! \brief reserved field */
        final int[] reserved;

        ModelParam(float base_score, int num_feature, ModelReader reader) throws IOException {
            this.base_score = base_score;
            this.num_feature = num_feature;
            this.num_class = reader.readInt();
            this.saved_with_pbuffer = reader.readInt();
            this.contain_eval_metrics = reader.readInt();
            this.major_version = reader.readUnsignedInt();
            this.minor_version = reader.readUnsignedInt();
            this.reserved = reader.readIntArray(27);
        }
    }

    public GradBooster getBooster(){
        return gbm;
    }

    public String getObjName() {
        return name_obj;
    }

    public float getBaseScore() {
        return base_score;
    }

}
