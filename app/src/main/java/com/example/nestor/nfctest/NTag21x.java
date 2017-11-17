package com.example.nestor.nfctest;

import android.nfc.Tag;
import android.nfc.tech.NfcA;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by nestor on 16/11/17.
 */

public class NTag21x extends Exception {

    private static final String TAG = NTag21x.class.getCanonicalName();

    Tag tag;
    NfcA nfcA;

    // User Memory (It's defined in the children)
    protected byte PAGE_USER_START;
    protected byte PAGE_USER_END;


    // Available CMDs
    private static final byte READ = (byte) 0x30;
    private static final byte FAST_READ = (byte) 0x3A;
    private static final byte WRITE = (byte) 0xA2;
    private static final byte PWD_AUTH = (byte)  0x1B;
    private NTag21xException e;

    public NTag21x(Tag tag) {
        this.tag = tag;
        this.nfcA = NfcA.get(tag);
    }

    public void setInterfaceException(NTag21xException e) {
        this.e = e;
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
        while (i < response.length && response[i] != (byte) 0xFF) {
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
            copy[i] = (byte) 0xFF;
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

    public void formatMemory() throws Exception {
        byte currentPage = PAGE_USER_START;
        for (int j = PAGE_USER_START; j <= PAGE_USER_END; j++) {
            writePage(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, currentPage);
            currentPage += (byte) 0x01;
        }
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
}
