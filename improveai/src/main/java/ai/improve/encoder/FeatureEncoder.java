package ai.improve.encoder;

import java.util.*;

import ai.improve.log.IMPLog;
import biz.k11i.xgboost.util.FVec;
import com.sun.source.tree.BreakTree;

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

    public HashMap<String, Integer> featureIndexes;

    public List<StringTable> stringTables;

    private List<StringTable> internalStringTables;

    private StringTable sharedStringTable;

    private static final String Tag = "FeatureEncoder";

    public double noise;

//    private Map<String, Integer> featureNamesMap = new HashMap<>();
//
//    private long variantSeed;
//
//    private long valueSeed;
//
//    private long givensSeed;
//
//    // TODO this is obsolete
//    private boolean warningOnceArrayEncoding = false;

    public FeatureEncoder(List<String> featureNames, Map<String, List<Long>> stringTables, long model_seed){
        this.featureIndexes = new HashMap<>();

        this.internalStringTables = new ArrayList<>(featureNames.size());
        this.sharedStringTable = new StringTable(new ArrayList<>(), model_seed);

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
                int dummy = 0;
//                System.out.println("obj type: " + obj + "| dummy int type: " + (Double) dummy);
                into[featureIndex] = sprinkle(((Number) obj).doubleValue(), noiseShift, noiseScale);
            }

        } else if (obj instanceof String) {
            if (!featureIndexes.containsKey(path)) {
                return;
            }
            int featureIndex = featureIndexes.get(path);
            System.out.println("feature index: " + featureIndex);
            into[featureIndex] = sprinkle(internalStringTables.get(featureIndex).encode((String) obj), noiseShift, noiseScale);
        } else if (obj instanceof List) {

            List<Object> objList = (List<Object>) obj;

            for (int i = 0; i < ((List<?>) obj).size(); i++) {
                encode(objList.get(i), path + "." + i, into, noiseShift, noiseScale);
            }

        } else if (obj instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>)obj).entrySet()) {
                if(!(entry.getKey() instanceof String)) {
                    IMPLog.w(Tag, "Map entry ignored: map key must be of type String.");
                    continue;
                }
                encode(entry.getValue(), path + "." + entry.getKey(), into, noiseShift, noiseScale);
            }

        } else if (obj == null) {
            // for null do nothing

        } else {
            throw new RuntimeException("unsupported type <" + obj.getClass().getCanonicalName() + ">, not JSON encodeable." +
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

    //    public <T> List<FVec> encodeVariants(List<T> variants, Map givens) {
//        double noise = Double.isNaN(this.noise) ? Math.random() : this.noise;
//
//        double[] givensFeature = givens != null ? encodeGivens(givens, noise) : null;
//
//        List<FVec> result = new ArrayList(variants.size());
//        for (Object variant: variants) {
//            double[] variantFeatures;
//            if(givensFeature != null) {
//                variantFeatures = Arrays.copyOf(givensFeature, givensFeature.length);
//            } else {
//                variantFeatures = new double[featureNamesMap.size()];
//                Arrays.fill(variantFeatures, Double.NaN);
//            }
//            encodeVariant(variant, noise, variantFeatures);
//            result.add(FVec.Transformer.fromArray(variantFeatures, false));
//        }
//        return result;
//    }


    // TODO does that even make sense for tests' sake?
    public List<FVec> encodeItemsForPrediction(List<Object> items, Object context, double noise) {
        List<FVec> encodedFeaturesFVecs = new ArrayList<>(items.size());

        for (int i = 0; i < items.size(); ++i) {
            // Object item, Object context, double[] into, double noise
            double[] recordInto = new double[this.featureIndexes.size()];
            Arrays.fill(recordInto, Double.NaN);
            encodeFeatureVector(items.get(i), context, recordInto, noise);
            encodedFeaturesFVecs.add(FVec.Transformer.fromArray(recordInto, false));
            }
        return encodedFeaturesFVecs;
        }

//    public static native long  xxhash3(byte[] data, long seed);
//
//    static {
//        System.loadLibrary("xxhash");
//    }

//    public FeatureEncoder(long modelSeed, List<String> featureNames) {
//        variantSeed = xxhash3("variant".getBytes(), modelSeed);
//        valueSeed = xxhash3("$value".getBytes(), variantSeed);//$value
//        givensSeed = xxhash3("givens".getBytes(), modelSeed);
//
//        noise = Double.NaN;
//
//        if(featureNames != null) {
//            for (int i = 0; i < featureNames.size(); ++i) {
//                featureNamesMap.put(featureNames.get(i), i);
//            }
//        } else {
//            IMPLog.e(Tag, "featureNames is null, is this a valid model?");
//        }
//    }
//
//    public <T> List<FVec> encodeVariants(List<T> variants, Map givens) {
//        double noise = Double.isNaN(this.noise) ? Math.random() : this.noise;
//
//        double[] givensFeature = givens != null ? encodeGivens(givens, noise) : null;
//
//        List<FVec> result = new ArrayList(variants.size());
//        for (Object variant: variants) {
//            double[] variantFeatures;
//            if(givensFeature != null) {
//                variantFeatures = Arrays.copyOf(givensFeature, givensFeature.length);
//            } else {
//                variantFeatures = new double[featureNamesMap.size()];
//                Arrays.fill(variantFeatures, Double.NaN);
//            }
//            encodeVariant(variant, noise, variantFeatures);
//            result.add(FVec.Transformer.fromArray(variantFeatures, false));
//        }
//        return result;
//    }
//
//    private double[] encodeGivens(Object givens, double noise) {
//        double[] features = new double[featureNamesMap.size()];
//        Arrays.fill(features, Double.NaN);
//        double smallNoise = shrink(noise);
//        return encodeInternal(givens, givensSeed, smallNoise, features);
//    }
//
//    private double[] encodeVariant(Object variant, double noise, double[] features) {
//        double smallNoise = shrink(noise);
//        if(variant instanceof Map) {
//            return encodeInternal(variant, variantSeed, smallNoise, features);
//        } else {
//            return encodeInternal(variant, valueSeed, smallNoise, features);
//        }
//    }
//
//    private double[] encodeInternal(Object node, long seed, double noise, double[] features) {
//        if(node instanceof Boolean) {
//            double nodeValue = ((Boolean)node).booleanValue() ? 1.0 : 0.0;
//            String featureName = hash_to_feature_name(seed);
////            IMPLog.d(Tag, "featureName: "+featureName);
//            if(featureNamesMap.containsKey(featureName)) {
//                int index = featureNamesMap.get(featureName);
//                double unsprinkled = 0;
//                if(!Double.isNaN(features[index])) {
//                    unsprinkled = reverseSprinkle(features[index], noise);
//                }
//                features[index] = sprinkle(nodeValue+unsprinkled, noise);
//            }
//        } else if(node instanceof Number) {
//            if(!Double.isNaN(((Number)node).doubleValue())) {
//                double nodeValue = ((Number) node).doubleValue();
//                String featureName = hash_to_feature_name(seed);
////                IMPLog.d(Tag, "featureName: " + featureName);
//                if (featureNamesMap.containsKey(featureName)) {
//                    int index = featureNamesMap.get(featureName);
//                    double unsprinkled = 0;
//                    if (!Double.isNaN(features[index])) {
//                        unsprinkled = reverseSprinkle(features[index], noise);
//                    }
//                    features[index] = sprinkle(nodeValue + unsprinkled, noise);
//                }
//            }
//        } else if(node instanceof String) {
//            long hashed = xxhash3(((String) node).getBytes(), seed);
//            String featureName = hash_to_feature_name(seed);
////            IMPLog.d(Tag, "featureName: " + featureName);
//            if(featureNamesMap.containsKey(featureName)) {
//                int index = featureNamesMap.get(featureName);
//                double unsprinkled = 0.0;
//                if(!Double.isNaN(features[index])) {
//                    unsprinkled = reverseSprinkle(features[index], noise);
//                }
//                features[index] = sprinkle(unsprinkled + ((hashed & 0xffff0000L) >>> 16) - 0x8000, noise);
//            }
//
//            String hashedFeatureName = hash_to_feature_name(hashed);
////            IMPLog.d(Tag, "featureName: "+hashedFeatureName);
//            if(featureNamesMap.containsKey(hashedFeatureName)) {
//                int index = featureNamesMap.get(hashedFeatureName);
//                double unsprinkled = 0.0;
//                if(!Double.isNaN(features[index])) {
//                    unsprinkled = reverseSprinkle(features[index], noise);
//                }
//                features[index] = sprinkle(unsprinkled + (hashed & 0xffffL) - 0x8000, noise);
//            }
//        } else if (node instanceof Map) {
//            for (Map.Entry<String, Object> entry : ((Map<String, Object>)node).entrySet()) {
//                if(!(entry.getKey() instanceof String)) {
//                    IMPLog.w(Tag, "Map entry ignored: map key must be of type String.");
//                    continue;
//                }
//                long newSeed = xxhash3(entry.getKey().getBytes(), seed);
//                encodeInternal(entry.getValue(), newSeed, noise, features);
//            }
//        } else if (node instanceof List) {
//            if(!warningOnceArrayEncoding) {
//                warningOnceArrayEncoding = true;
//                IMPLog.w(Tag, "Array encoding may change in the near future.");
//            }
//            List list = (List)node;
//            for (int i = 0; i < list.size(); ++i) {
//                byte[] bytes = longToByteArray(i);
//                long newSeed = xxhash3(bytes, seed);
//                encodeInternal(list.get(i), newSeed, noise, features);
//            }
//        } else if(node == null) {
//            // do nothing
//        } else {
//            throw new RuntimeException("unsupported type <" + node.getClass().getCanonicalName() + ">, not JSON encodeable." +
//                    " Must be one of type map, list, string, number, boolean, or null");
//        }
//        return features;
//    }
//
//    final static char[] ref = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
//    public String hash_to_feature_name(long hash) {
//        char[] buffer = new char[8];
//        int hash32 = (int)(hash >>> 32);
//        buffer[0] = ref[((hash32 >>> 28) & 0xf)];
//        buffer[1] = ref[((hash32 >>> 24) & 0xf)];
//        buffer[2] = ref[((hash32 >>> 20) & 0xf)];
//        buffer[3] = ref[((hash32 >>> 16) & 0xf)];
//        buffer[4] = ref[((hash32 >>> 12) & 0xf)];
//        buffer[5] = ref[((hash32 >>> 8) & 0xf)];
//        buffer[6] = ref[((hash32 >>> 4) & 0xf)];
//        buffer[7] = ref[((hash32) & 0xf)];
//        return new String(buffer);
//    }
//
//    private double shrink(double noise) {
//        return noise * Math.pow(2, -17);
//    }
//
//    private double sprinkle(double x, double small_noise) {
//        return (x + small_noise) * (1 + small_noise);
//    }
//
//    private double reverseSprinkle(double sprinkled, double small_noise) {
//        return sprinkled / (1 + small_noise) - small_noise;
//    }
//
//    private final byte[] longToByteArray(long value) {
//        return new byte[] {
//                (byte)(value >> 56),
//                (byte)(value >> 48),
//                (byte)(value >> 40),
//                (byte)(value >> 32),
//                (byte)(value >> 24),
//                (byte)(value >> 16),
//                (byte)(value >> 8),
//                (byte)value};
//    }
//
//    public static native long  xxhash3(byte[] data, long seed);
//
//    static {
//        System.loadLibrary("xxhash");
//    }
}
