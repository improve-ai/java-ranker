package ai.improve.encoder;

import java.util.*;

import biz.k11i.xgboost.util.FVec;

public class FeatureEncoder {

    /**
     * Feature names prefix for features derived from candidates / items, e.g.:
     * - item == 1 -> feature name is "item"
     * - item == [1] -> feature names is "item.0"
     * - item == {"a": 1}} - feature name is "item.a"
     */
    public static final String ITEM_FEATURE_KEY = "item";

    /**
     * Feature names prefix for features derived from context, e.g.:
     * - context == 1 -> feature name is "context"
     * - context == [1] -> feature names is "context.0"
     * - context == {"a": 1}} - feature name is "context.a"
     */
    public static final String CONTEXT_FEATURE_KEY = "context";

    /**
     * A mapping containing feature name -> feature index pairs
     */
    public HashMap<String, Integer> featureIndexes;

    /**
     * A list of StringTable objects for each feature
     */
    private final List<StringTable> internalStringTables;

    /**
     * Creates a new FeatureEncoder instance
     * @param featureNames a list of feature names
     * @param stringTables a map of <string feature name> : <list of target - value hashes for string feature>
     * @param model_seed a non-negative 32 bit int used for xxhash3
     */
    public FeatureEncoder(List<String> featureNames, Map<String, List<Long>> stringTables, long model_seed){
        this.featureIndexes = new HashMap<>();

        this.internalStringTables = new ArrayList<>(featureNames.size());
        StringTable sharedStringTable = new StringTable(new ArrayList<>(), model_seed);

        for (String featureName: featureNames) {
            this.featureIndexes.put(featureName, this.featureIndexes.size());
            internalStringTables.add(sharedStringTable);
        }

        for (Map.Entry<String, List<Long>> jsonStringTable: stringTables.entrySet()) {
            if (!this.featureIndexes.containsKey(jsonStringTable.getKey())) {
                throw new NoSuchElementException("Bad model metadata");
            }

            internalStringTables.set(
                    featureIndexes.get(jsonStringTable.getKey()),
                    new StringTable(jsonStringTable.getValue(), model_seed));
        }

    }

    /**
     * Encodes provided item to `input` double[]
     * @param item a JSON encodable object (one of candidates / items) to be encoded with item path prefix
     * @param into a double[] into which values will be added / inserted
     * @param noiseShift a small bias value added to each of values encoded form item
     * @param noiseScale a small multiplier for each of values encoded form item
     */
    public void encodeItem(Object item, double[] into, double noiseShift, double noiseScale) {
        encode(item, ITEM_FEATURE_KEY, into, noiseShift, noiseScale);
    }

    /**
     * Encodes provided context to `input` double[]
     * @param context a JSON encodable context object to be encoded
     * @param into a double[] into which values will be added / inserted
     * @param noiseShift a small bias value added to each of values encoded form item
     * @param noiseScale a small multiplier for each of values encoded form item
     */
    public void encodeContext(Object context, double[] into, double noiseShift, double noiseScale) {
        encode(context, CONTEXT_FEATURE_KEY, into, noiseShift, noiseScale);
    }

    /**
     * Fully encodes provided variant and context into a np.ndarray provided as
     * `into` parameter. `into` must not be None
     * @param item a JSON encodable item to be encoded
     * @param context a JSON encodable context to be encoded
     * @param into a double[] into which values will be added / inserted
     * @param noise value in [0, 1) which will be combined with the feature value
     */
    public void encodeFeatureVector(Object item, Object context, double[] into, double noise) {

        double[] noiseShiftAndScale = getNoiseShiftAndScale(noise);

        if (item != null) {
            encodeItem(item, into, noiseShiftAndScale[0], noiseShiftAndScale[1]);
        }

        if (context != null) {
            encodeContext(context, into, noiseShiftAndScale[0], noiseShiftAndScale[1]);
        }
    }

    public List<FVec> encodeFeatureVectors(List<?> items, Object context, double noise) {
        List<FVec> result = new ArrayList<>();
        for(int i = 0; i < items.size(); ++i) {
            double[] fvalues = new double[this.featureIndexes.size()];
            Arrays.fill(fvalues, Double.NaN);
            encodeFeatureVector(items.get(i), context, fvalues, noise);
            result.add(FVec.Transformer.fromArray(fvalues, false));
        }
        return result;
    }

    /**
     * Encodes a JSON serializable object to a float vector
     * Rules of encoding go as follows:
     *  - None, json null, {}, [], and nan are treated as missing features and ignored.
     *  - numbers and booleans are encoded as-is.
     *  - strings are encoded using a lookup table
     * @param obj a JSON encodable object to be encoded
     * @param path the JSON-normalized path to the current object
     * @param into xgboost predictor FVec object
     * @param noiseShift a small bias value added to each of values encoded form item
     * @param noiseScale a small multiplier for each of values encoded form item
     */
    private void encode(Object obj, String path, double[] into, double noiseShift, double noiseScale) {
        if (obj instanceof Boolean || obj instanceof Number) {
            if (!featureIndexes.containsKey(path)) {
                return;
            }
            int featureIndex = featureIndexes.get(path);

            if (obj instanceof Boolean) {
                into[featureIndex] = sprinkle((Boolean) obj ? 1.0 : 0.0, noiseShift, noiseScale);
            } else {
                into[featureIndex] = sprinkle(((Number) obj).doubleValue(), noiseShift, noiseScale);
            }

        } else if (obj instanceof String) {
            if (!featureIndexes.containsKey(path)) {
                return;
            }
            int featureIndex = featureIndexes.get(path);
            into[featureIndex] = sprinkle(internalStringTables.get(featureIndex).encode((String) obj), noiseShift, noiseScale);
        } else if (obj instanceof List) {

            List<Object> objList = (List<Object>) obj;

            for (int i = 0; i < ((List<?>) obj).size(); i++) {
                encode(objList.get(i), path + "." + i, into, noiseShift, noiseScale);
            }

        } else if (obj instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>)obj).entrySet()) {
                if(!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Map keys must be String.");
                }
                encode(entry.getValue(), path + "." + entry.getKey(), into, noiseShift, noiseScale);
            }

        } else if (obj == null || obj.equals(null)) {
            // for null do nothing
        } else {
            throw new IllegalArgumentException("unsupported type <" + obj.getClass().getCanonicalName() + ">, not JSON encodable." +
                    " Must be one of type map, list, string, number, boolean, or null");
        }
    }

    /**
     * Returns noise shift (small value added to feature value) and noise scale
     * (value by which shifted feature value is multiplied)
     * @param noise value in [0, 1) which will be combined with the feature value
     * @return a sequence with 2 floats: [noise_shift, noise_scale]
     */
    public static double[] getNoiseShiftAndScale(double noise) {
        return new double[] {noise * Math.pow(2.0, -142.0), 1.0 + noise * Math.pow(2.0, -17.0)};
    }

    /**
     * Adds noise shift and scales shifted value
     * @param x value to be sprinkled
     * @param noiseShift small bias added to the feature value
     * @param noiseScale small multiplier of the shifted feature value
     * @return sprinkled double value
     */
    public static double sprinkle(double x, double noiseShift, double noiseScale) {
        return (x + noiseShift) * noiseScale;
    }
}
