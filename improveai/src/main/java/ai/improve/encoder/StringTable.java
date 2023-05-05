package ai.improve.encoder;

import java.util.List;
import java.util.HashMap;

import java.util.Collections;


public class StringTable {

    /**
     * 32-bit integer model seed used with xxhash to encode strings
     */
    public long modelSeed;


    /**
     * 32 bit int mask used for string encoding, e.g. 000...11
     */
    public int mask;


    /**
     * Double value representing snap / width of the 'miss interval' - numeric interval into which all missing / unknown
     * values are encoded. It is 0-centered.
     */
    public double missWidth;

    /**
     * target -> value encoding hash table
     */
    public HashMap<Long, Double> valueTable;


    public StringTable(List<Long> jsonStringTable, int modelSeed){
        // init modelSeed param
        this.modelSeed = modelSeed;
        // set mask for xxhash string encoding
        this.mask = getMask(jsonStringTable);
        int maxPosition = jsonStringTable.size() - 1;

        // empty and single entry tables will have a miss_width of 1 or range [-0.5, 0.5]
        // 2 / max_position keeps miss values from overlapping with nonzero table values
        this.missWidth = ((maxPosition < 1) ? 1 : 2 / (double) maxPosition);
        this.valueTable = new HashMap<>();

        // copy list but not elements
        List<Long> reversedStringTable = jsonStringTable.subList(0, jsonStringTable.size());
        // reverse copied inplace
        Collections.reverse(reversedStringTable);
        // iterate over reversed string table to populate target - value encoding hash table
        for (int i = 0; i < reversedStringTable.size(); i++) {
            this.valueTable.put(
                    reversedStringTable.get(i),
                    (maxPosition == 0.0) ? 1.0 : StringTable.scale( (double) i / (double) maxPosition, this.missWidth));
        }

    }


    /**
     * Encodes string hash as a miss
     * @param stringHash string hash to be encoded as a miss
     * @return encoded miss value within miss width
     */
    public double encodeMiss(long stringHash){
        return scale((stringHash & 0xFFFFFFFF) * Math.pow(2.0, -32.0), this.missWidth);
    }


    /**
     * Encode input string to a target value
     * @param string string to encode
     * @return encoded value
     */
    public double encode(String string){
        // compute xxhash3 for input string
        long stringHash = xxh3(string.getBytes(), this.modelSeed);

        // compute mask hash because it will be used twice
        long maskedHashedString = stringHash & this.mask;

        // if value present in target-value encoding hash table, return it
        if (this.valueTable.containsKey(maskedHashedString)) {
            return this.valueTable.get(maskedHashedString);
        }

        // return miss
        return this.encodeMiss(stringHash);
    }

    /**
     * helper method to calculate log2
     * @param x value to calculate log2 of
     * @return value of log2(x)
     */
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }


    /**
     * helper method to calculate log2
     * @param stringTable a list of integers representing string encoding for a given string feature
     * @return uint32 mask used for string encoding
     */
    public static int getMask(List<Long> stringTable){
        if (stringTable.size() == 0) {
            return 0;
        }

        long maxValue = Collections.max(stringTable);
        if (maxValue == 0) {
            return 0;
        }

        return (1 << (int) (StringTable.log2(maxValue) + 1)) - 1;
    }


    /**
     * Scales input miss value to [-width/2, width/2]. Assumes input is within [0, 1] range.
     * @param val value to scale into [-width/2, width/2] range
     * @param width miss range width
     * @return scaled value
     */
    public static double scale(double val, double width){
        if (width < 0){
            throw new IllegalArgumentException("width must be positive");
        }
        return width * (val - 0.5);

    }

    public static native long  xxh3(byte[] data, long seed);

    static {
        System.loadLibrary("xxhash");
    }
}
