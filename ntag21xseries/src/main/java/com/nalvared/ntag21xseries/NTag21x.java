package com.nalvared.ntag21xseries;

import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by nestor on 16/11/17.
 */

/**
 * Specification document available in:
 * https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf
 */

public class NTag21x extends Exception {

    private static final String TAG = NTag21x.class.getCanonicalName();

    Tag tag;
    NfcA nfcA;
    boolean debugMode = false;

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

    // Write access is protected by the password verification
    public static final int FLAG_ONLY_WRITE = 0;
    public static final int FLAG_READ_WRITE = 1;

    private static final byte[] ONLY_WRITE = new byte[]{ 0x00, 0x00, 0x00, 0x00 };
    private static final byte[] READ_WRITE = new byte[]{ (byte) 0x80, 0x00, 0x00, 0x00 };
    private static final byte[] ALL_PAGES = new byte[]{ 0x04, 0x00, 0x00, 0x00 };
    private static final byte[] NO_PAGES = new byte[]{ 0x04, 0x00, 0x00, (byte) 0xFF };

    // Error codes
    private final static int ERROR_CONNECTION = 0;
    private final static int ERROR_TRANSCEIVE = 1;
    private final static int ERROR_MAX_CAPACITY = 2;
    private final static int ERROR_AUTHENTICATION_VERIFY = 3;

    public NTag21x(Tag tag) {
        this.tag = tag;
        this.nfcA = NfcA.get(tag);
    }

    /**
     * Enables debug mode for seeing the errors trace
     * @param enable activates print stack of the errors
     */
    public void debugMode(boolean enable) {
        debugMode = enable;
    }

    /**
     * Connect with the Tag if it's necessary
     * @param eventListener
     * @return if it has been connected
     */
    public boolean connect(NTag21xEventListener eventListener) {
        if (!nfcA.isConnected()) {
            try {
                nfcA.connect();
                eventListener.OnSuccess("Connection is now established");
            } catch (IOException e) {
                eventListener.OnError(e.getMessage(), ERROR_CONNECTION);
                e.printStackTrace();
            }
        }
        return nfcA.isConnected();
    }

    /**
     * Close the connection with the Tag if it's necessary
     * @param eventListener
     * @return if it has been closed
     */
    public boolean close(NTag21xEventListener eventListener) {
        if (nfcA.isConnected()) {
            try {
                nfcA.close();
                eventListener.OnSuccess("Connection is now closed");
            } catch (IOException e) {
                eventListener.OnError(e.getMessage(), ERROR_CONNECTION);
                e.printStackTrace();
            }
        }
        return nfcA.isConnected();
    }

    /**
     * Defines the time of the connection
     * Normally it is used for large operations o many of them
     * @param millis is the time in milliseconds
     */
    public void setTimeout(int millis) {
        nfcA.setTimeout(millis);
    }

    /**
     * Get Static ID (UID) of the Tag in byte array
     * @param eventListener
     */
    public void getStaticId(NTag21xEventListener eventListener) {
        try {
            byte[] response = nfcA.transceive(new byte[] {
                    READ,
                    (byte)0x00
            });
            eventListener.OnSuccess(Arrays.copyOf(response,7));
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), ERROR_TRANSCEIVE);
            e.printStackTrace();
        }
    }

    private byte[] readMemory(NTag21xEventListener eventListener) {
        try {
            byte[] response = nfcA.transceive(new byte[] {
                    FAST_READ,
                    PAGE_USER_START,
                    PAGE_USER_END
            });
            return response;
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), ERROR_TRANSCEIVE);
            if (debugMode)
                e.printStackTrace();
        }
        return null;
    }

    public void getUserMemory(NTag21xEventListener eventListener) {
        byte[] response = readMemory(eventListener);
        if (response != null)
            eventListener.OnSuccess(response);
    }

    public void read(NTag21xEventListener eventListener) {
        byte[] response = readMemory(eventListener);
        int i = 0;
        while (i < response.length && response[i] != (byte) 0x00) {
            i += 1;
        }
        if (response != null)
            eventListener.OnSuccess(Arrays.copyOf(response, i));
    }

    private byte[] writeMemoryPage(byte[] page, byte origin, NTag21xEventListener eventListener) {
        try {
            byte[] result = nfcA.transceive(new byte[] {
                    WRITE,
                    origin,
                    page[0], page[1], page[2], page[3]
            });
            return result;
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), ERROR_TRANSCEIVE);
            if(debugMode) e.printStackTrace();
        } catch (IllegalStateException e) {
            eventListener.OnError(e.getMessage(), ERROR_TRANSCEIVE);
            if(debugMode) e.printStackTrace();
        }
        return null;
    }

    public void write(byte[] pages, NTag21xEventListener eventListener) {
        if (pages.length / 4 > PAGE_USER_END - PAGE_USER_START + 1) {
            eventListener.OnError("The length of message exceeded the tag capacity",
                    ERROR_MAX_CAPACITY);
            return;
        }
        formatMemory(eventListener);
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
            writeMemoryPage(curr, currentPage, eventListener);
            currentPage += (byte) 0x01;
        }
        eventListener.OnSuccess("The writing operation has been completed successfully");
    }

    public void writeAndTrucate(byte[] pages, NTag21xEventListener eventListener) {
        if (pages.length / 4 > PAGE_USER_END - PAGE_USER_START + 1) {
            pages = Arrays.copyOf(pages, (PAGE_USER_END - PAGE_USER_START + 1) * 4);
            write(pages, eventListener);
        }
        write(pages, eventListener);
    }

    public void authAndWrite(byte[] pwd, byte[] pack, byte[] pages,
                             NTag21xEventListener eventListener) {
        int isNeeded = -2;
        if ((isNeeded = needAuthentication(eventListener)) != -1) {
            if (isNeeded == -2) {
                return;
            }
            byte[] response = new byte[0];
            try {
                response = nfcA.transceive(new byte[]{
                        PWD_AUTH,
                        pwd[0], pwd[1], pwd[2], pwd[3]
                });
            } catch (IOException e) {
                if (debugMode) e.printStackTrace();
            }
            if (response[0] == pack[0] && response[1] == pack[1]) {
                write(pages, eventListener);
            }
        }
    }

    public void formatMemory(NTag21xEventListener eventListener) {
        byte currentPage = PAGE_USER_START;
        for (int j = PAGE_USER_START; j <= PAGE_USER_END; j++) {
            writeMemoryPage(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00},
                    currentPage, eventListener);
            currentPage += (byte) 0x01;
        }
        eventListener.OnSuccess("Formatting successful");
    }

    /**
     * return codes:
     * -2: no identifiable
     * -1: the tag is not protected
     *  0: the tag is protected against write
     *  1: the tag is protected both read and write
     * @return code
     */
    public int needAuthentication(NTag21xEventListener eventListener) {
        try {
            byte[] response;
            response = nfcA.transceive(new byte[]{
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
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), ERROR_AUTHENTICATION_VERIFY);
            if (debugMode) e.printStackTrace();
        }
        return -2;
    }

    public void setPassword(byte[] pwd, byte[] pack, int flag,
                            NTag21xEventListener eventListener) {
        writeMemoryPage(pwd, PWD_CONFIG_PAGE, eventListener);
        writeMemoryPage(pack, PACK_CONFIG_PAGE, eventListener);
        if (flag == FLAG_ONLY_WRITE){
            writeMemoryPage(ONLY_WRITE, ACCESS_CONFIG_PAGE, eventListener);
        }
        else if (flag == FLAG_READ_WRITE) {
            writeMemoryPage(READ_WRITE, ACCESS_CONFIG_PAGE, eventListener);
        }
        writeMemoryPage(ALL_PAGES, AUTH0_CONFIG_PAGE, eventListener);
        eventListener.OnSuccess("Password successfully assigned");
    }

    public void removePassword(byte[] pwd, byte[] pack, NTag21xEventListener eventListener) {
        byte[] response = new byte[0];
        try {
            response = nfcA.transceive(new byte[]{
                    PWD_AUTH,
                    pwd[0], pwd[1], pwd[2], pwd[3]
            });
        } catch (IOException e) {
           if (debugMode) e.printStackTrace();
        }
        if (response[0] == pack[0] && response[1] == pack[1])
            writeMemoryPage(NO_PAGES, AUTH0_CONFIG_PAGE, eventListener);
        eventListener.OnSuccess("Password successfully removed");
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

    private String responseNAK(byte[] nak) {
        switch (nak[0]){
            case 0xA:
                return "Acknowledge (ACK)";
            case 0x0:
                return "NAK for invalid argument (i.e. invalid page address)";
            case 0x1:
                return "NAK for parity or CRC error";
            case 0x4:
                return "NAK for invalid authentication counter overflow";
            case 0x5:
                return "NAK for EEPROM write error";
        }
        return null;
    }
}
