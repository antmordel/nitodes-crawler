package dev.nito.crawlernodes.protocol.types;

import lombok.Value;

/**
 * Source: https://en.bitcoin.it/wiki/Protocol_documentation#Variable_length_string
 */
@Value
public class VarStr {

    private final String value;

    public byte[] getBytes() {
        return new byte[0];
    }
}
