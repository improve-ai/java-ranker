package ai.improve.encoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.log.IMPLog;
import biz.k11i.xgboost.util.FVec;

public class FeatureEncoder {
    private static final String Tag = "FeatureEncoder";

    public double noise;

    private Map<String, Integer> featureNamesMap = new HashMap<>();

    private long variantSeed;

    private long valueSeed;

    private long givensSeed;

    public FeatureEncoder(long modelSeed, List<String> featureNames) {
        variantSeed = xxhash3("variant".getBytes(), modelSeed);
        valueSeed = xxhash3("$value".getBytes(), variantSeed);//$value
        givensSeed = xxhash3("givens".getBytes(), modelSeed);

        noise = Double.NaN;

        if(featureNames != null) {
            for (int i = 0; i < featureNames.size(); ++i) {
                featureNamesMap.put(featureNames.get(i), i);
            }
        } else {
            IMPLog.e(Tag, "featureNames is null, is this a valid model?");
        }
    }

    public <T> List<FVec> encodeVariants(List<T> variants, Map givens) {
        double noise = Double.isNaN(this.noise) ? Math.random() : this.noise;

        double[] givensFeature = givens != null ? encodeGivens(givens, noise) : null;

        List<FVec> result = new ArrayList(variants.size());
        for (Object variant: variants) {
            double[] variantFeatures;
            if(givensFeature != null) {
                variantFeatures = givensFeature;
            } else {
                variantFeatures = new double[featureNamesMap.size()];
                Arrays.fill(variantFeatures, Double.NaN);
            }
            encodeVariant(variant, noise, variantFeatures);
            result.add(FVec.Transformer.fromArray(variantFeatures, false));
        }
        return result;
    }

    private double[] encodeGivens(Object givens, double noise) {
        double[] features = new double[featureNamesMap.size()];
        Arrays.fill(features, Double.NaN);
        double smallNoise = shrink(noise);
        return encodeInternal(givens, givensSeed, smallNoise, features);
    }

    private double[] encodeVariant(Object variant, double noise, double[] features) {
        double smallNoise = shrink(noise);
        if(variant instanceof Map) {
            return encodeInternal(variant, variantSeed, smallNoise, features);
        } else {
            return encodeInternal(variant, valueSeed, smallNoise, features);
        }
    }

    private double[] encodeInternal(Object node, long seed, double noise, double[] features) {
        if(node instanceof Boolean) {
            double nodeValue = ((Boolean)node).booleanValue() ? 1.0 : 0.0;
            String featureName = hash_to_feature_name(seed);
            IMPLog.d(Tag, "featureName: "+featureName);
            if(featureNamesMap.containsKey(featureName)) {
                int index = featureNamesMap.get(featureName);
                double unsprinkled = 0;
                if(!Double.isNaN(features[index])) {
                    unsprinkled = reverseSprinkle(features[index], noise);
                }
                features[index] = sprinkle(nodeValue+unsprinkled, noise);
            }
        } else if(node instanceof Number && !Double.isNaN(((Number)node).doubleValue())) {
            double nodeValue = ((Number)node).doubleValue();
            String featureName = hash_to_feature_name(seed);
            IMPLog.d(Tag, "featureName: "+featureName);
            if(featureNamesMap.containsKey(featureName)) {
                int index = featureNamesMap.get(featureName);
                double unsprinkled = 0;
                if(!Double.isNaN(features[index])) {
                    unsprinkled = reverseSprinkle(features[index], noise);
                }
                features[index] = sprinkle(nodeValue+unsprinkled, noise);
            }
        } else if(node instanceof String) {
            long hashed = xxhash3(((String) node).getBytes(), seed);
            String featureName = hash_to_feature_name(seed);
            IMPLog.d(Tag, "featureName: " + featureName);
            if(featureNamesMap.containsKey(featureName)) {
                int index = featureNamesMap.get(featureName);
                double unsprinkled = 0.0;
                if(!Double.isNaN(features[index])) {
                    unsprinkled = reverseSprinkle(features[index], noise);
                }
                features[index] = sprinkle(unsprinkled + ((hashed & 0xffff0000L) >>> 16) - 0x8000, noise);
            }

            String hashedFeatureName = hash_to_feature_name(hashed);
            IMPLog.d(Tag, "featureName: "+hashedFeatureName);
            if(featureNamesMap.containsKey(hashedFeatureName)) {
                int index = featureNamesMap.get(hashedFeatureName);
                double unsprinkled = 0.0;
                if(!Double.isNaN(features[index])) {
                    unsprinkled = reverseSprinkle(features[index], noise);
                }
                features[index] = sprinkle(unsprinkled + (hashed & 0xffffL) - 0x8000, noise);
            }
        } else if (node instanceof Map) {
            for (Map.Entry<String, Object> entry : ((HashMap<String, Object>)node).entrySet()) {
                if(!(entry.getKey() instanceof String)) {
                    IMPLog.w(Tag, "Map entry ignored: map key must be of type String.");
                    continue;
                }
                long newSeed = xxhash3(entry.getKey().getBytes(), seed);
                encodeInternal(entry.getValue(), newSeed, noise, features);
            }
        } else if (node instanceof List) {
            List list = (List)node;
            for (int i = 0; i < list.size(); ++i) {
                byte[] bytes = longToByteArray(i);
                long newSeed = xxhash3(bytes, seed);
                encodeInternal(list.get(i), newSeed, noise, features);
            }
        } else {
            throw new RuntimeException("unsupported type <" + node.getClass().getCanonicalName() + ">, not JSON encodeable." +
                    " Must be one of type map, list, string, number, boolean, or null");
        }
        return features;
    }

    public String hash_to_feature_name(long hash) {
        return String.format("%08x", (int)(hash >>> 32));
    }

    private double shrink(double noise) {
        return noise * Math.pow(2, -17);
    }

    private double sprinkle(double x, double small_noise) {
        return (x + small_noise) * (1 + small_noise);
    }

    private double reverseSprinkle(double sprinkled, double small_noise) {
        return sprinkled / (1 + small_noise) - small_noise;
    }

    private final byte[] longToByteArray(long value) {
        return new byte[] {
                (byte)(value >> 56),
                (byte)(value >> 48),
                (byte)(value >> 40),
                (byte)(value >> 32),
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value};
    }

    public static native long  xxhash3(byte[] data, long seed);

    static {
        System.loadLibrary("xxhash");
    }
}
