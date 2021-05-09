package ai.improve.android.hasher;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XXFeatureEncoder {
    private static final String Tag = "XXFeatureEncoder";

    static {
        System.loadLibrary("xxhash");
    }

    public native long  xxhash3(byte[] data, long seed);

    public boolean testMode;

    public double noise;

    private long mModelSeed;

    private long mVariantSeed;

    private long mValueSeed;

    private long mContextSeed;

    public XXFeatureEncoder(long modelSeed) {
        mModelSeed = modelSeed;
        mVariantSeed = xxhash3("variant".getBytes(), mModelSeed);
        mValueSeed = xxhash3("$value".getBytes(), mVariantSeed);//$value
        mContextSeed = xxhash3("context".getBytes(), mModelSeed);

        byte[] xx = "$value".getBytes();

        Log.d(Tag, "max long " + Long.MAX_VALUE);

        Log.d(Tag, "variantSeed="+mVariantSeed + ", valueSeed=" + mValueSeed + ", contextSeed=" + mContextSeed);
    }

    public List<Map> encodeVariants(List<Object> variants, Map context){
        double noise = testMode ? this.noise : Math.random();

        Map<String, Double> contextFeature = context != null ? encodeContext(context, noise) : null;

        List<Map> result = new ArrayList(variants.size());
        for (Object variant: variants) {
            Map variantFeatures = (contextFeature != null) ? contextFeature : new HashMap();
            result.add(encodeVariant(variant, noise, variantFeatures));
        }
        return result;
    }

    private Map<String, Double> encodeContext(Object context, double noise) {
        Map<String, Double> features = new HashMap();
        double smallNoise = shrink(noise);
        return encodeInternal(context, mContextSeed, smallNoise, features);
    }

    private Map<String, Double> encodeVariant(Object variant, double noise, Map<String, Double> features) {
        double smallNoise = shrink(noise);
        if(variant instanceof Map) {
            return encodeInternal(variant, mVariantSeed, smallNoise, features);
        } else {
            return encodeInternal(variant, mValueSeed, smallNoise, features);
        }
    }

    private Map<String, Double> encodeInternal(Object node, long seed, double noise, Map<String, Double> features) {
        if(node instanceof Boolean) {
            double nodeValue = ((Boolean)node).booleanValue() ? 1.0 : 0.0;
            String featureName = hash_to_feature_name(seed);
            Double curValue = features.get(featureName);
            if(curValue != null) {
                features.put(featureName, curValue + sprinkle(nodeValue, noise));
            } else {
                features.put(featureName, sprinkle(nodeValue, noise));
            }
        } else if(node instanceof Number && (((Number)node).doubleValue() != Double.NaN)) {
            String featureName = hash_to_feature_name(seed);
            Double curValue = features.get(featureName);
            if(curValue != null) {
                features.put(featureName, curValue + sprinkle((Double) node, noise));
            } else {
                features.put(featureName, sprinkle((Double) node, noise));
            }
        } else if(node instanceof String) {
            long hashed = xxhash3(((String) node).getBytes(), seed);
            String featureName = hash_to_feature_name(seed);
            Double curValue = features.get(featureName);
            if(curValue != null) {
                features.put(featureName, curValue + sprinkle(((hashed & 0xffff0000) >> 16) - 0x8000, noise));
            } else {
                features.put(featureName, sprinkle(((hashed & 0xffff0000) >> 16) - 0x8000, noise));
            }

            String hashedFeatureName = hash_to_feature_name(hashed);
            Double curHashedValue = features.get(hashedFeatureName);
            if(curHashedValue != null) {
                features.put(featureName, curValue + sprinkle((hashed & 0xffff) - 0x8000, noise));
            } else {
                features.put(featureName, sprinkle((hashed & 0xffff) - 0x8000, noise));
            }
        } else if (node instanceof Map) {
            for (Map.Entry<String, Object> entry : ((HashMap<String, Object>)node).entrySet()) {
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
        return String.format("%x", hash >> 32);
    }

    private double shrink(double noise) {
        return noise * Math.pow(2, -17);
    }

    private double sprinkle(double x, double small_noise) {
        return (x + small_noise) * (1 + small_noise);
    }

    private final byte[] longToByteArray(long value) {
        return new byte[] {
                (byte)(value >>> 56),
                (byte)(value >>> 48),
                (byte)(value >>> 40),
                (byte)(value >>> 32),
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
}
