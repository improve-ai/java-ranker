package ai.improve.android.hasher;

import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.util.FastMath;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature Encoder
 * <TBD> Business logic definitions are required
 */
public class FeatureEncoder {

    private static final int TBL_IDX_COLUMNS = 0;
    private static final int TBL_IDX_VALUES = 1;


    /**
     * Lookup table for feature compilation
     * Data Structure:
     * [
     * [0,1,2], // Values used to look up specific column in a sub-table below
     * [ // sub-table with lookup values
     * [
     * [0,1], // sub-table column lookup
     * [0,1,2,3,4] // sub-table values used in feature calculation
     * ]
     * ]
     */
    private final List<Number> table;

    /**
     * Model seed value
     */
    private final int modelSeed;


    /**
     * Constructor
     *
     * @param table     - List of lookup table values (refer to data structure above)
     * @param modelSeed - model seed
     */
    public FeatureEncoder(List<Number> table, int modelSeed) {
        this.table = table;
        this.modelSeed = modelSeed;
    }

    public Map<Integer, Double> encodeFeatures(Object data) {
        Map<String, Object> jsonData = Collections.singletonMap("json", data);
        return encodeFeatures(jsonData, null);
    }

    public Map<Integer, Double> encodeFeatures(Object data, Map<Integer, Double> context) {
        Map<String, Object> jsonData = Collections.singletonMap("json", data);
        return encodeFeatures(jsonData, context);
    }


    /**
     * Main method encoding JSON-formatted data into
     *
     * @param jsonData - JSON-formatted data to analyze
     * @return - feature map in form of Integer:Double pairs
     */
    public Map<Integer, Double> encodeFeatures(Map<String, Object> jsonData) {
        return encodeFeatures(jsonData, null);
    }

    public Map<Integer, Double> encodeFeatures(Map<String, Object> jsonData, Map<Integer, Double> initialContext) {
        Map<String, Object> flatData = flattenJsonData(jsonData);
        return encodeFeaturesFromFlattenedData(flatData, initialContext);
    }

    /**
     * Encodes features from pre-flattened JSON data
     *
     * @param flatData - flattened JSON data
     * @param initialContext initial context features
     * @return Feature map in form of Integer:Double pairs
     */
    private Map<Integer, Double> encodeFeaturesFromFlattenedData(Map<String, Object> flatData, Map<Integer, Double> initialContext) {
        Map<Integer, Double> features = new HashMap<>();
        if(initialContext != null) {
            features.putAll(initialContext);
        }

        double noise = new JDKRandomGenerator().nextGaussian();
        for (Map.Entry<String, Object> entry : flatData.entrySet()) {
            int column = lookupColumn(entry.getKey(), modelSeed);
            Object v = entry.getValue();
            if (v == null) {
                continue;
            }
            if (v instanceof Boolean) {
                double value = (((Boolean) v) ? 1.0 : 0.0);
                features.put(column, value);
            } else if (v instanceof Number) {
                features.put(column, ((Number) v).doubleValue());
            } else if (v instanceof String) {
                features.put(column, lookupValue(column, (String) v, modelSeed, noise));
            }
        }
        return features;
    }

    /**
     * Simple murmurhash wrapper to return result equivalent to Python's `mmh3.hash(key, model_seed, signed=False)`
     *
     * @param value     Value to hash
     * @param modelSeed model seed
     * @return Unsigned Murmurhash32 value
     */
    private long unsignedMmh3Hash(String value, int modelSeed) {
        long hash = MurmurHash3.hash32x86(value.getBytes(), 0, value.length(), modelSeed);
        return hash & 0x0ffffffffl; //convert to unsigned
    }

    /**
     * Convenience method with minimal / default parameters
     *
     * @param key       - key to look up
     * @param modelSeed - model seed value
     * @return - hashed column index value
     */
    private int lookupColumn(String key, int modelSeed) {
        return lookupColumn(this.table, key, modelSeed, 1);
    }

    /**
     * Look up correct column in the table by applying hash calculations
     *
     * @param lookupTable - table to look up values in
     * @param key         - key to look up
     * @param modelSeed   - model seed value
     * @param w           - arbitrary parameter
     * @return
     */
    private int lookupColumn(List<Number> lookupTable, String key, int modelSeed, int w) {
        List<Number> columns = (List<Number>) lookupTable.get(TBL_IDX_COLUMNS);
        List<Number> values = (List<Number>) lookupTable.get(TBL_IDX_VALUES);
        long hash = unsignedMmh3Hash(key, modelSeed);
        int columnIndex = (int) (hash % columns.size());
        int columnValue = columns.get(columnIndex).intValue();

        if (columnValue < 0) {
            return Math.abs(columnValue) - 1;
        } else {
            return (int) (unsignedMmh3Hash(key, (int) (columnValue ^ modelSeed)) % (FastMath.floorDiv(values.size(), w)));
        }
    }

    /**
     * Look up value in subtables (refer to data structure definition)
     *
     * @param column    - column index in values array
     * @param key       - key to look up
     * @param modelSeed - model seed
     * @param noise     - pre-defined noise value
     * @return
     */
    private Double lookupValue(int column, String key, int modelSeed, double noise) {
        List<Number> values = (List<Number>) table.get(TBL_IDX_VALUES);
        List<Number> subtable = (List<Number>) values.get(column);
        List<Number> subvalues = (List<Number>) subtable.get(TBL_IDX_VALUES);

        int subcolumn = lookupColumn(subtable, key, modelSeed, 2) * 2;
        double result = subvalues.get(subcolumn).doubleValue() + (subvalues.get(subcolumn + 1).doubleValue() * noise);
        return result;
    }

    /**
     * Returns flattened JSON object, usint \0 (null character) as a separator.
     * Example: {"test": {"some": "value"}} => {"test\0some": "value}
     *
     * @param jsonData JSON string to parse / flatten
     * @return HashMap with flattened JSON data structure
     */
    private Map<String, Object> flattenJsonData(Map<String, Object> jsonData) {
        Map<String, Object> result = MapFlattener.flatten(jsonData, "\0");
        return result;
    }


}