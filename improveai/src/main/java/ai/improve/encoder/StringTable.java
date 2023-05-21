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
    public long mask;


    /**
     * Double value representing snap / width of the 'miss interval' - numeric interval into which all missing / unknown
     * values are encoded. It is 0-centered.
     */
    public double missWidth;

    /**
     * target -> value encoding hash table
     */
    public HashMap<Long, Double> valueTable;

    private static final String Tag = "StringTable";


    public StringTable(List<Long> jsonStringTable, long modelSeed){
        // init modelSeed param
        // make sure that input modelSeed is a valid result of pythonic random.getrandbits(32)
        if (modelSeed < 0) {
            throw new IllegalArgumentException("Provided modelSeed must be greater than 0 ");
        }
        this.modelSeed = modelSeed;

        // set mask for xxhash string encoding
        this.mask = getMask(jsonStringTable);

        // get max position in string table
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
                    (maxPosition == 0.0) ? 1.0 : StringTable.scale( (double) i / (double) maxPosition));
        }

    }


    /**
     * Encodes string hash as a miss
     * @param stringHash string hash to be encoded as a miss
     * @return encoded miss value within miss width
     */
    public double encodeMiss(long stringHash){
        return scale((double) (stringHash & 0xFFFFFFFFL) * Math.pow(2.0, -32.0), this.missWidth);
    }


    /**
     * Encode input string to a target value
     * @param string string to encode
     * @return encoded value
     */
    public double encode(String string){
        // compute xxhash3 for input string
        long stringHash = xxhash3(string.getBytes(), this.modelSeed);

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


    // TODO test getMask()
    /**
     * helper method to calculate log2
     * @param stringTable a list of integers representing string encoding for a given string feature
     * @return uint32 mask used for string encoding
     */
    public static long getMask(List<Long> stringTable){
        if (stringTable.size() == 0) {
            return 0;
        }

        long maxValue = Collections.max(stringTable);
        if (maxValue == 0) {
            return 0;
        }

        return ((long) 1 << (long) ((StringTable.log2(maxValue) + 1.0))) - 1;
    }


    /**
     * Scales input miss value to [-1, 1] (miss width defaults to 2.0). Assumes input is within [0, 1] range.
     * @param val value to scale into [-1, 1] range
     * @return scaled value
     */
    public static double scale(double val){
        return StringTable.scale(val, 2.0);
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

    public static native long xxhash3(byte[] data, long seed);

    static {
        System.loadLibrary("xxhash");
    }
}
