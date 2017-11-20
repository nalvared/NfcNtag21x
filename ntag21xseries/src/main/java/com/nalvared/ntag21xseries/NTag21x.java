package com.nalvared.ntag21xseries;

import android.nfc.Tag;
import android.nfc.tech.NfcA;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by nestor on 16/11/17.
 */

/**
 * Specification document available in:
 * https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf
 */

public class NTag21x extends Exception
        implements NTag21xException{

    private static final String TAG = NTag21x.class.getCanonicalName();

    Tag tag;
    NfcA nfcA;

    // User Memory (It's defined in the children)
    protected byte PAGE_USER_START;
    protected byte PAGE_USER_END;
    protected byte AUTH0_CONFIG_PAGE;
    protected byte ACCESS_CONFIG_PAGE;
    protected byte PWD_CONFIG_PAGE;
    protected byte PACK_CONFIG_PAGE;


    // Available CMDs
    private static final byte READ = (byte) 0x30;
    private static final byte FAST_READ = (byte) 0x3A;
    private static final byte WRITE = (byte) 0xA2;
    private static final byte PWD_AUTH = (byte)  0x1B;

    // Mirror page (00 00 00 00 | 00 00 00 FF)
    // If you like protects all pages AUTH0 should be 00
    // If you don't like protect any page AUTH0 should be FF
    /**
     * MIRROR       04h; need UID ASCII mirror_conf
     * RFUI         00h
     * MIRROR_PAGE  00h
     * AUTH0        00h ... FFh, defines the page address from which
     *                           the password verification is required
     */

    // Access page (00 00 00 00 | 80 00 00 00)
    // By default with no limiting of wrong passwords
    /**
     * ACCESS        00h | 80h
     *  PROT                1b = 0 | 1; flag for protection (0 write; 1 read and write)
     *  CFGLCK              1b = 0
     *  RFUI                1b = 0
     *  NFC_CNT_EN          1b = 0
     *  NFC_CNT_PWD_PROT    1b = 0
     *  AUTHLIM             3b = 000 | (001 ... 111); limiting of negative password
     *                                verification attempts disabled
     * RFUI          00h
     * RFUI          00h
     * RFUI          00h
     */

    // Write access is protected by the password verification

    public static final int FLAG_ONLY_WRITE = 0;
    public static final int FLAG_READ_WRITE = 1;

    private static final byte[] ONLY_WRITE = new byte[]{
            0x00,
            0x00,
            0x00,
            0x00
    };
    private static final byte[] READ_WRITE = new byte[]{
            (byte) 0x80,
            0x00,
            0x00,
            0x00
    };

    private static final byte[] ALL_PAGES = new byte[]{
            0x04,
            0x00,
            0x00,
            0x00
    };

    private static final byte[] NO_PAGES = new byte[]{
            0x04,
            0x00,
            0x00,
            (byte) 0xFF
    };


    private NTag21xException e;

    public NTag21x(Tag tag) {
        this.tag = tag;
        this.nfcA = NfcA.get(tag);
        this.e = this;
    }

    public void connect() throws IOException {
        nfcA.connect();
    }

    public void close() throws IOException {
        nfcA.close();
    }

    public void setTimeout(int millis) throws IOException {
        nfcA.setTimeout(millis);
    }

    public byte[] getUidBytes() throws Exception {
        if (!nfcA.isConnected()) {
            e.isNotConnected();
            return null;
        }
        byte[] response;
        response = nfcA.transceive(new byte[] {
                READ,
                (byte)0x00
        });
        byte[] uid = Arrays.copyOf(response,7);
        return uid;
    }

    public String getUid() throws Exception {
        return bytesToHex(getUidBytes());
    }

    public byte[] getUserMemory() throws Exception {
        if (!nfcA.isConnected()) {
            e.isNotConnected();
            return null;
        }
        byte[] response;
        response = nfcA.transceive(new byte[] {
                FAST_READ,
                PAGE_USER_START,
                PAGE_USER_END
        });
        return response;
    }

    public byte[] read() throws Exception {
        if (!nfcA.isConnected()) {
            e.isNotConnected();
            return null;
        }
        byte[] response = getUserMemory();
        int i = 0;
        while (i < response.length && response[i] != (byte) 0x00) {
            i += 1;
        }
        return Arrays.copyOf(response, i);
    }

    public void writePage(byte[] page, byte origin) throws Exception {
        if (!nfcA.isConnected()) {
            e.isNotConnected();
            return;
        }
        nfcA.transceive(new byte[] {
                WRITE,
                origin,
                page[0], page[1], page[2], page[3]
        });
    }

    public void write(byte[] pages) throws Exception {
        if (pages.length / 4 > PAGE_USER_END - PAGE_USER_START + 1) {
            e.pageUserExceeded(pages.length);
            return;
        }
        formatMemory();
        int len = pages.length;
        while (len % 4 != 0) {
            len += 1;
        }
        byte[] copy = new byte[len];
        int i = 0;
        for (byte b : pages) {
            copy[i] = b;
            i += 1;
        }
        while (i < len) {
            copy[i] = (byte) 0x00;
            i += 1;
        }
        byte currentPage = PAGE_USER_START;
        for (int j = 0; j < len; j += 4) {
            byte[] curr = Arrays.copyOfRange(copy, j, j + 4);
            writePage(curr, currentPage);
            currentPage += (byte) 0x01;
        }
    }

    public boolean writeToTheLimit(byte[] pages) throws Exception {
        if (pages.length / 4 > PAGE_USER_END - PAGE_USER_START + 1) {
            pages = Arrays.copyOf(pages, (PAGE_USER_END - PAGE_USER_START + 1) * 4);
            write(pages);
            return false;
        }
        write(pages);
        return true;
    }

    public void authAndWrite(byte[] pwd, byte[] pack, byte[] pages) throws Exception {
        int isNeeded = -2;
        if ((isNeeded = needAuthentication()) != -1) {
            if (isNeeded == -2) {
                e.isNotConnected();
                return;
            }
            byte[] response = authentication(pwd);
            if (response[0] == pack[0] && response[1] == pack[1]) {
                write(pages);
            }
        }
    }

    public void formatMemory() throws Exception {
        byte currentPage = PAGE_USER_START;
        for (int j = PAGE_USER_START; j <= PAGE_USER_END; j++) {
            writePage(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}, currentPage);
            currentPage += (byte) 0x01;
        }
    }

    /**
     * return codes:
     * -2: no identifiable
     * -1: the tag is not protected
     *  0: the tag is protected against write
     *  1: the tag is protected both read and write
     * @return code
     * @throws Exception
     */
    public int needAuthentication() throws Exception {
        if (!nfcA.isConnected()) {
            e.isNotConnected();
            return -2;
        }
        byte[] response = nfcA.transceive(new byte[]{
                READ,
                AUTH0_CONFIG_PAGE
        });
        if(response != null && response.length == 16) {
            if(response[3] != (byte) 0xFF) {
                byte access = response[4];
                char prot = String.format("%8s", Integer.toBinaryString(access & 0xFF))
                        .replace(' ', '0').charAt(0);
                if (prot == '0') {
                    return 0;
                }
                else if (prot == '1') {
                    return 1;
                }
            }
            return -1;
        }
        return -2;
    }

    public byte[] getAuthConfigPage() throws Exception {
        if (!nfcA.isConnected()) {
            e.isNotConnected();
            return null;
        }
        return nfcA.transceive(new byte[]{
                READ,
                AUTH0_CONFIG_PAGE
        });
    }

    public byte[] authentication(byte[] pwd) throws Exception {
        return nfcA.transceive(new byte[]{
                PWD_AUTH,
                pwd[0], pwd[1], pwd[2], pwd[3]
        });
    }

    public void setPassword(byte[] pwd, byte[] pack, int flag) throws Exception {
        writePage(pwd, PWD_CONFIG_PAGE);
        writePage(pack, PACK_CONFIG_PAGE);
        if (flag == FLAG_ONLY_WRITE){
            writePage(ONLY_WRITE, ACCESS_CONFIG_PAGE);
        }
        else if (flag == FLAG_READ_WRITE) {
            writePage(READ_WRITE, ACCESS_CONFIG_PAGE);
        }
        writePage(ALL_PAGES, AUTH0_CONFIG_PAGE);
    }

    public void removePassword(byte[] pwd, byte[] pack) throws Exception {
        byte[] response = authentication(pwd);
        if (response[0] == pack[0] && response[1] == pack[1])
            writePage(NO_PAGES, AUTH0_CONFIG_PAGE);
    }

    private String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // [BEGIN] - Implementing interface
    private static final String E_USER_PAGE_LIMIT = "Writing %d bytes and the Tag has only %d bytes";
    private static final String E_TIMEOUT = "Tag must be connected (Check timeout for large operations)";

    @Override
    public void isNotConnected() throws Exception {
        throw new Exception(E_TIMEOUT);
    }

    @Override
    public void pageUserExceeded(int max) throws Exception {
        int bytes = (PAGE_USER_END - PAGE_USER_START) * 4;
        throw new Exception(String.format(E_USER_PAGE_LIMIT, max, bytes));
    }
    // [ END ] - Implementing interface
}
