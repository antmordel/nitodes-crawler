package dev.nito.crawlernodes.protocol.types;

import lombok.Value;

/**
 * Source: https://en.bitcoin.it/wiki/Protocol_documentation#Variable_length_integer
 *
 * Integer can be encoded depending on the represented value to save space.
 * Variable length integers always precede an array/vector of a type of data that may vary in length.
 * Longer numbers are encoded in little endian.
 */
@Value
public class VarInt {
    private final int size;

    public byte[] getBytes() {
        return new byte[0];
    }

    /**
     * Returns the minimum encoded size of the given unsigned long value.
     *
     *      Value	        Storage length	Format
     *      < 0xFD	        1	            uint8_t
     *      <= 0xFFFF	    3	            0xFD followed by the length as uint16_t
     *      <= 0xFFFF FFFF	5	            0xFE followed by the length as uint32_t
     *      -	            9	            0xFF followed by the length as uint64_t
     */
    private static int sizeOf(long value) {
        // if negative, it's actually a very large unsigned long value
        if (value < 0) return 9; // 1 marker + 8 data bytes
        if (value < 253) return 1; // 1 data byte
        if (value <= 0xFFFFL) return 3; // 1 marker + 2 data bytes
        if (value <= 0xFFFFFFFFL) return 5; // 1 marker + 4 data bytes
        return 9; // 1 marker + 8 data bytes
    }
}
