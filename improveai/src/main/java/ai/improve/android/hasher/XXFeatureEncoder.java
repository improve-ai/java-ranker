package ai.improve.android.hasher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.android.IMPLog;
import biz.k11i.xgboost.util.FVec;

public class XXFeatureEncoder {
    private static final String Tag = "XXFeatureEncoder";

    static {
        System.loadLibrary("xxhash");
    }

    public native long  xxhash3(byte[] data, long seed);

    public boolean testMode;

    public double noise;

    private Map<String, Integer> featureNamesMap = new HashMap<>();

    private long variantSeed;

    private long valueSeed;

    private long contextSeed;

    public XXFeatureEncoder(long modelSeed, List<String> featureNames) {
        variantSeed = xxhash3("variant".getBytes(), modelSeed);
        valueSeed = xxhash3("$value".getBytes(), variantSeed);//$value
        contextSeed = xxhash3("context".getBytes(), modelSeed);
        for(int i = 0; i < featureNames.size(); ++i) {
            featureNamesMap.put(featureNames.get(i), i);
        }
        if(featureNamesMap.size() != featureNames.size()) {
            // This won't happen unless the model is nvalid.
        }
    }

    public List<FVec> encodeVariants(List<Object> variants, Object context) {
        double noise = testMode ? this.noise : Math.random();

        double[] contextFeature = context != null ? encodeContext(context, noise) : null;

        List<FVec> result = new ArrayList(variants.size());
        for (Object variant: variants) {
            double[] variantFeatures = (contextFeature != null) ? contextFeature : new double[featureNamesMap.size()];
            encodeVariant(variant, noise, variantFeatures);
            result.add(FVec.Transformer.fromArray(variantFeatures, true));
        }
        return result;
    }

    private double[] encodeContext(Object context, double noise) {
        double[] features = new double[featureNamesMap.size()];
        double smallNoise = shrink(noise);
        return encodeInternal(context, contextSeed, smallNoise, features);
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
            if(featureNamesMap.containsKey(featureName)) {
                int index = featureNamesMap.get(featureName);
                features[index] += sprinkle(nodeValue, noise);
            }
        } else if(node instanceof Number && !Double.isNaN(((Number)node).doubleValue())) {
            double nodeValue = ((Number)node).doubleValue();
            String featureName = hash_to_feature_name(seed);
            if(featureNamesMap.containsKey(featureName)) {
                int index = featureNamesMap.get(featureName);
                features[index] += sprinkle(nodeValue, noise);
            }
        } else if(node instanceof String) {
            long hashed = xxhash3(((String) node).getBytes(), seed);
            String featureName = hash_to_feature_name(seed);
            if(featureNamesMap.containsKey(featureName)) {
                int index = featureNamesMap.get(featureName);
                features[index] += sprinkle(((hashed & 0xffff0000L) >>> 16) - 0x8000, noise);
            }

            String hashedFeatureName = hash_to_feature_name(hashed);
            if(featureNamesMap.containsKey(hashedFeatureName)) {
                int index = featureNamesMap.get(hashedFeatureName);
                features[index] += sprinkle((hashed & 0xffffL) - 0x8000, noise);
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
        }
        return features;
    }

    private String hash_to_feature_name(long hash) {
        return String.format("%x", (int)(hash >>> 32));
    }

    private double shrink(double noise) {
        return noise * Math.pow(2, -17);
    }

    private double sprinkle(double x, double small_noise) {
        return (x + small_noise) * (1 + small_noise);
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
}
