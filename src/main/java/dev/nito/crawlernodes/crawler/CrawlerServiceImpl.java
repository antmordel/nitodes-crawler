package dev.nito.crawlernodes.crawler;

import com.google.common.net.InetAddresses;
import dev.nito.crawlernodes.ByteUtils;
import dev.nito.crawlernodes.protocol.types.VarStr;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class CrawlerServiceImpl implements CrawlerService {

    private static final String MY_NODE_IP = "46.101.94.174";
    private static final int MY_NODE_PORT = 8333;

    /* ------ FORMAT OF A MESSAGE ------
    source: https://en.bitcoin.it/wiki/Protocol_documentation

    -- Message Header
    ------------------
    4   magic_number    uint32
    12  command         char
    4   payload_length  uint32
    4	checksum        uint32
    ?   payload         char

        -- Version message
        4	version         int32    # 70015 for this implementation
        8	services	    uint64	 # local services that this node offers
        8	timestamp	    int64    # Standard UNIX timestamp in seconds
        26	addr_recv	    net_addr # Bitcoin protocol ignores this...
        26	addr_from	    net_addr # Bitcoin protocol ignores this...
        8	nonce	        uint64   # Node random nonce, randomly generated every time a version packet is sent. This nonce is used to detect connections to self.
        ?	user_agent	    var_str	 # bitcrawlerj :)
        4	start_height    int32
        1	relay	        bool


        -- *net_addr type consists of
        4	time	    uint32	    the Time (version >= 31402). Not present in VersionMessage!
        8	services	uint64_t	same service(s) listed in version
        16	IPv6/4	    char[16]
        2	port	    uint16_t

     */

    private static final long MAGIC_NUMBER = 0xF9BEB4D9L; // magic number for mainnet

    private static final long PROTOCOL_VERSION = 70015L;
    private static final int LOCAL_SERVICES = 0;

    public static byte[] versionPayload() {

        long version = PROTOCOL_VERSION;
        long localServices = LOCAL_SERVICES;
        long timestamp = System.currentTimeMillis() / 1000;

        // net_addr :: The target peer address information
        InetAddress localhost = InetAddresses.forString("127.0.0.1");
        // TODO is there any tool to map IPv4 addresses to IPv6 in Java?
        byte[] ipBytesAddrRecv = localhost.getAddress();
        if (ipBytesAddrRecv.length == 4) {
            byte[] v6addr = new byte[16];
            System.arraycopy(ipBytesAddrRecv, 0, v6addr, 12, 4);
            v6addr[10] = (byte) 0xFF;
            v6addr[11] = (byte) 0xFF;
            ipBytesAddrRecv = v6addr;
        }

        // net_addr :: The local address information
        // InetAddress localhost = InetAddresses.forString("127.0.0.1");
        byte[] ipBytesAddrFrom = localhost.getAddress();
        if (ipBytesAddrFrom.length == 4) {
            byte[] v6addr = new byte[16];
            System.arraycopy(ipBytesAddrFrom, 0, v6addr, 12, 4);
            v6addr[10] = (byte) 0xFF;
            v6addr[11] = (byte) 0xFF;
            ipBytesAddrFrom = v6addr;
        }

        // At this stage we don't care about connections to ourselves
        long nonce = 0L;
        VarStr userAgent = new VarStr("crawler-nodes");
        long bestKnownHeight = 300_000L;
        boolean relay = true;

        // **************************************
        // dumping all the info to the byte array
        // **************************************
        int totalVersionPayloadLength = 4 + 8 + 8 + 26 + 26 + 8 + userAgent.getBytes().length + 4 + 1;
        byte[] result = new byte[totalVersionPayloadLength];

        int cursor = 0;
        ByteUtils.uint32ToByteArrayLE(version, result, cursor);
        cursor += 4;
        ByteUtils.uint64ToByteArrayLE(localServices, result, cursor);
        cursor += 8;
        ByteUtils.uint64ToByteArrayLE(timestamp, result, cursor);
        cursor += 8;
        // addr_recv
        ByteUtils.uint64ToByteArrayLE(localServices, result, cursor);
        cursor += 8;
        System.arraycopy(ipBytesAddrRecv, 0, result, cursor, 16);
        cursor += 16;
        ByteUtils.uint16ToByteStreamBE(MY_NODE_PORT, result, cursor);
        cursor += 2;
        // addr_from
        ByteUtils.uint64ToByteArrayLE(localServices, result, cursor);
        cursor += 8;
        System.arraycopy(ipBytesAddrFrom, 0, result, cursor, 16);
        cursor += 16;
        ByteUtils.uint16ToByteStreamBE(MY_NODE_PORT, result, cursor);
        cursor += 2;

        ByteUtils.uint64ToByteArrayLE(nonce, result, cursor);
        cursor += 8;
        // user agent
        System.arraycopy(userAgent.getBytes(), 0, result, cursor, userAgent.getBytes().length);
        cursor += userAgent.getBytes().length;

        ByteUtils.uint32ToByteArrayLE(bestKnownHeight, result, cursor);
        cursor += 4;
        result[cursor] = 1;

        return result;
    }

    public static byte[] header() throws NoSuchAlgorithmException {
        byte[] bytesVersionPayload = versionPayload();
        String command = "version";
        long payloadLength = bytesVersionPayload.length;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] doubleChecksum = digest.digest(digest.digest(bytesVersionPayload));

//        4   magic_number    uint32
//        12  command         char
//        4   payload_length  uint32
//        4	  checksum        uint32
        int headerLength = 4 + 12 + 4 + 4;
        byte[] result = new byte[headerLength];
        int cursor = 0;
        ByteUtils.uint32ToByteArrayBE(MAGIC_NUMBER, result, cursor);
        cursor += 4;
        for (int i = 0; i < command.length() && i < 12; i++) {
            result[cursor + i] = (byte) (command.codePointAt(i) & 0xFF);
        }
        cursor += 12;
        ByteUtils.uint32ToByteArrayLE(payloadLength, result, cursor);
        cursor += 4;
        System.arraycopy(doubleChecksum, 0, result, cursor, 4);

        return result;
    }

    @Override
    public void start() {
        log.info("Starting daemon...");
        // connect to my server
        try (Socket clientSocket = new Socket(MY_NODE_IP, MY_NODE_PORT)) {

            byte[] header = header();
            byte[] versionPayload = versionPayload();

            OutputStream out = clientSocket.getOutputStream();
            InputStreamReader in = new InputStreamReader(clientSocket.getInputStream());

            out.write(header);
            out.write(versionPayload);
            out.flush();

            System.out.println("Resibi algooo: " + in.read());

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Socket could not connect. " + e.getMessage());
        }
    }
}
