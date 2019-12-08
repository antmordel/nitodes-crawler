package dev.nito.crawlernodes;

import dev.nito.crawlernodes.crawler.CrawlerServiceImpl;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Main {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static void main(String[] args) throws NoSuchAlgorithmException {

        byte[] versionPayload = CrawlerServiceImpl.versionPayload();
        byte[] header = CrawlerServiceImpl.header();

        System.out.println("Version payload");
        System.out.println(Arrays.toString(versionPayload));
        System.out.println(bytesToHex(versionPayload));

        System.out.println("Header");
        System.out.println(Arrays.toString(header));
        System.out.println(bytesToHex(header));
    }
}
