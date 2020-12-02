/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ai.improve.android.hasher;

/**
 * Minimal version of Murmur3 hasher from Guava implementation
 *
 * Compared to MurmurHash3 from commons-codec, this version returns the same results
 * as Python code when handling UTF-8 characters.
 */
public class GuavaMmh3Hasher {

    private static final int CHUNK_SIZE = 4;

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private final int seed;

    public GuavaMmh3Hasher(int seed) {
        this.seed = seed;
    }

    public int bits() {
        return 32;
    }

    @Override
    public String toString() {
        return "Hashing.murmur3_32(" + seed + ")";
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof GuavaMmh3Hasher) {
            GuavaMmh3Hasher other = (GuavaMmh3Hasher) object;
            return seed == other.seed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() ^ seed;
    }
    private static final int UNSIGNED_MASK = 0xFF;

    public static int toInt(byte value) {
        return value & UNSIGNED_MASK;
    }

    public HashCode hashBytes(byte[] input) {
        return hashBytes(input, 0, input.length);
    }

    public HashCode hashBytes(byte[] input, int off, int len) {
        checkPositionIndexes(off, off + len, input.length);
        int h1 = seed;
        int i;
        for (i = 0; i + CHUNK_SIZE <= len; i += CHUNK_SIZE) {
            int k1 = mixK1(getIntLittleEndian(input, off + i));
            h1 = mixH1(h1, k1);
        }

        int k1 = 0;
        for (int shift = 0; i < len; i++, shift += 8) {
            k1 ^= toInt(input[off + i]) << shift;
        }
        h1 ^= mixK1(k1);
        return fmix(h1, len);
    }

    public static void checkPositionIndexes(int start, int end, int size) {
        // Carefully optimized for execution by hotspot (explanatory comment above)
        if (start < 0 || end < start || end > size) {
            throw new IndexOutOfBoundsException();
        }
    }
    /**
     * Returns the {@code int} value whose byte representation is the given 4 bytes, in big-endian
     * order; equivalent to {@code Ints.fromByteArray(new byte[] {b1, b2, b3, b4})}.
     *
     * @since 7.0
     */
    public static int fromBytes(byte b1, byte b2, byte b3, byte b4) {
        return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
    }

    private static int getIntLittleEndian(byte[] input, int offset) {
        return fromBytes(input[offset + 3], input[offset + 2], input[offset + 1], input[offset]);
    }

    private static int mixK1(int k1) {
        k1 *= C1;
        k1 = Integer.rotateLeft(k1, 15);
        k1 *= C2;
        return k1;
    }

    private static int mixH1(int h1, int k1) {
        h1 ^= k1;
        h1 = Integer.rotateLeft(h1, 13);
        h1 = h1 * 5 + 0xe6546b64;
        return h1;
    }

    // Finalization mix - force all bits of a hash block to avalanche
    private static HashCode fmix(int h1, int length) {
        h1 ^= length;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        return HashCode.fromInt(h1);
    }


}
