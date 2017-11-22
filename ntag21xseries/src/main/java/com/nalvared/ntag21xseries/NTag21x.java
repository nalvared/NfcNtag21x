package com.nalvared.ntag21xseries;

import android.nfc.Tag;
import android.nfc.tech.NfcA;

import java.io.IOException;
import java.util.Arrays;

/**
 * <H1>NTag21x</H1>
 *
 * <P>This class implements the main methods for interacting with
 * NFC NTag213, NTag215 and NTag216 models</P>
 *
 * <P>The difference with other libraries is that it allows used the authentication
 * properties of the these tags</P>
 *
 * <P>Note that {@link android.nfc.tech.NfcA} is used to interact with the tags</P>
 *
 * <P>Specification document available in:
 * <a href="https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf">
 *     https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf</a>
 * </P>
 *
 * <P>Néstor Álvarez Díaz, contact[at]nalvared.com</P>
 *
 * @author Néstor Álvarez Díaz
 * @version 1.0.1
 * @date 2017-11-16
 *
 */

public class NTag21x {

    private static final String TAG = NTag21x.class.getCanonicalName();

    private NfcA nfcA;
    private boolean debugMode = false;

    // Static ID flags
    public static final int UID_BYTES = 0;
    public static final int UID_SRTING = 1;

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


    /**
     * Constructor
     * @param tag is a raw Tag object
     */
    public NTag21x(Tag tag) {
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
     */
    public void connect() {
        if (!nfcA.isConnected()) {
            try {
                nfcA.connect();
            } catch (IOException e) {
                if (debugMode) e.printStackTrace();
            }
        }
    }

    /**
     * Close the connection with the Tag if it's necessary
     */
    public void close() {
        if (nfcA.isConnected()) {
            try {
                nfcA.close();
            } catch (IOException e) {
                if (debugMode) e.printStackTrace();
            }
        }
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
     * Get Static ID (UID) of the Tag
     * @param eventListener @see {@link NTagEventListener}
     * @param flag indicates if the OnSuccess event sends in bytes or in string format
     */
    public void getStaticId(int flag, NTagEventListener eventListener) {
        try {
            byte[] response = nfcA.transceive(new byte[] {
                    READ,
                    (byte)0x00
            });
            if (flag == UID_BYTES)
                eventListener.OnSuccess(Arrays.copyOf(response,7),
                        NTagEventListener.READ_STATIC_ID_BYTES);
            else if (flag == UID_SRTING)
                eventListener.OnSuccess(bytesToHex(Arrays.copyOf(response,7)),
                        NTagEventListener.READ_STATIC_ID_STRING);
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), NTagEventListener.ERROR_TRANSCEIVE);
            e.printStackTrace();
        }
    }

    /**
     * Read user memory
     * @param eventListener @see {@link NTagEventListener}
     * @return bytes read
     */
    private byte[] readMemory(NTagEventListener eventListener) {
        try {
            return nfcA.transceive(new byte[] {
                    FAST_READ,
                    PAGE_USER_START,
                    PAGE_USER_END
            });
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), NTagEventListener.ERROR_TRANSCEIVE);
            if (debugMode)
                e.printStackTrace();
        }
        return null;
    }

    /**
     * Get all user memory without filtering
     * @param eventListener @see {@link NTagEventListener}
     */
    public void getUserMemory(NTagEventListener eventListener) {
        byte[] response = readMemory(eventListener);
        if (response != null)
            eventListener.OnSuccess(response, NTagEventListener.READ_USER_MEMORY);
    }

    /**
     * Get used user memory, it filters by 0x00 byte as null
     * @param eventListener @see {@link NTagEventListener}
     */
    public void read(NTagEventListener eventListener) {
        byte[] response = readMemory(eventListener);
        int i = 0;
        assert response != null;
        while (i < response.length && response[i] != (byte) 0x00) {
            i += 1;
        }
        eventListener.OnSuccess(Arrays.copyOf(response, i), NTagEventListener.READ);
    }

    /**
     * Write page by page
     * @param page message to write 4 bytes
     * @param origin byte which indicates page for writing
     * @param eventListener @see {@link NTagEventListener}
     */
    private void writeMemoryPage(byte[] page, byte origin, NTagEventListener eventListener) {
        try {
            nfcA.transceive(new byte[]{
                    WRITE,
                    origin,
                    page[0], page[1], page[2], page[3]
            });
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), NTagEventListener.ERROR_TRANSCEIVE);
            if(debugMode) e.printStackTrace();
        } catch (IllegalStateException e) {
            eventListener.OnError(e.getMessage(), NTagEventListener.ERROR_TRANSCEIVE);
            if(debugMode) e.printStackTrace();
        }
    }

    /**
     * Writes full message from start page
     * It formats the tag first
     * @param pages message to write in bytes
     * @param eventListener @see {@link NTagEventListener}
     */
    public void write(byte[] pages, NTagEventListener eventListener) {
        if (pages.length / 4 > PAGE_USER_END - PAGE_USER_START + 1) {
            eventListener.OnError(NTagEventListener.ERROR_MAX_CAPACITY_MSG,
                    NTagEventListener.ERROR_MAX_CAPACITY);
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
        eventListener.OnSuccess(NTagEventListener.ON_WRITE_SUCCESS, NTagEventListener.WRITE);
    }

    /**
     * If the message is larger than space available, it truncates and writes
     * @param pages message to write in bytes
     * @param eventListener @see {@link NTagEventListener}
     */
    public void writeAndTrucate(byte[] pages, NTagEventListener eventListener) {
        if (pages.length / 4 > PAGE_USER_END - PAGE_USER_START + 1) {
            pages = Arrays.copyOf(pages, (PAGE_USER_END - PAGE_USER_START + 1) * 4);
            eventListener.OnError(NTagEventListener.ERROR_MAX_CAPACITY_MSG,
                    NTagEventListener.ERROR_MAX_CAPACITY);
            write(pages, eventListener);
        }
        write(pages, eventListener);
    }

    /**
     * Write the message after authenticating to the tag
     * @param pwd password 4 bytes
     * @param pack password acknowledgment 4 bytes
     * @param pages message to write in bytes
     * @param eventListener @see {@link NTagEventListener}
     */
    public void authAndWrite(byte[] pwd, byte[] pack, byte[] pages,
                             NTagEventListener eventListener) {
        int isNeeded = needAuthentication(eventListener);
        if (isNeeded == -2) {
            return;
        }
        if (isNeeded == -1) {
            write(pages, eventListener);
            return;
        }
        try {
            byte[] response;
            response = nfcA.transceive(new byte[]{
                    PWD_AUTH,
                    pwd[0], pwd[1], pwd[2], pwd[3]
            });
            if (response[0] == pack[0] && response[1] == pack[1]) {
                write(pages, eventListener);
            }
        } catch (IOException e) {
            if (debugMode) e.printStackTrace();
            eventListener.OnError(e.getMessage(), NTagEventListener.WRITE);
        }
    }

    /**
     * Put all user memory to 0x00
     * @param eventListener @see {@link NTagEventListener}
     */
    private void formatMemory(NTagEventListener eventListener) {
        byte currentPage = PAGE_USER_START;
        for (int j = PAGE_USER_START; j <= PAGE_USER_END; j++) {
            writeMemoryPage(new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00},
                    currentPage, eventListener);
            currentPage += (byte) 0x01;
        }
    }

    /**
     * Check if the tag needs authentication
     * return codes [RC]:
     * -2: no identifiable
     * -1: the tag is not protected
     *  0: the tag is protected against write
     *  1: the tag is protected both read and write
     * @return RC
     */
    private int needAuthentication(NTagEventListener eventListener) {
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
        } catch (IOException e) {
            eventListener.OnError(e.getMessage(), NTagEventListener.ERROR_AUTHENTICATION_VERIFY);
            if (debugMode) e.printStackTrace();
        }
        return -2;
    }

    /**
     * Check if tag needs authentication but it calls {@link NTagEventListener}
     * @param eventListener @see {@link NTagEventListener}
     */
    public void hasPassword(NTagEventListener eventListener) {
        int p = needAuthentication(eventListener);
        switch (p) {
            case -2:
                eventListener.OnSuccess(NTagEventListener.ERROR_PWD, -2);
                break;
            case -1:
                eventListener.OnSuccess(NTagEventListener.NO_PWD, -1);
                break;
            case 0:
                eventListener.OnSuccess(NTagEventListener.WRITE_ONLY_PWD, 0);
                break;
            case 1:
                eventListener.OnSuccess(NTagEventListener.RW_PWD, 1);
                break;
        }
    }

    /**
     * Set password to the tag, it requires that the tag does not have password enable
     * @param pwd password 4 bytes
     * @param pack password acknowledgment 2 bytes
     * @param flag authentication mode, ONLY_WRITE or READ_WRITE
     * @param eventListener @see {@link NTagEventListener}
     */
    public void setPassword(byte[] pwd, byte[] pack, int flag,
                            NTagEventListener eventListener) {
        byte[] nPack = new byte[4];
        nPack[0] = pack[0];
        nPack[1] = pack[1];
        nPack[2] = 0x00;
        nPack[3] = 0x00;

        writeMemoryPage(pwd, PWD_CONFIG_PAGE, eventListener);
        writeMemoryPage(nPack, PACK_CONFIG_PAGE, eventListener);
        if (flag == FLAG_ONLY_WRITE){
            writeMemoryPage(ONLY_WRITE, ACCESS_CONFIG_PAGE, eventListener);
        }
        else if (flag == FLAG_READ_WRITE) {
            writeMemoryPage(READ_WRITE, ACCESS_CONFIG_PAGE, eventListener);
        }
        writeMemoryPage(ALL_PAGES, AUTH0_CONFIG_PAGE, eventListener);
        eventListener.OnSuccess(NTagEventListener.ON_PASSWORD_ASSIGN, NTagEventListener.PWD_SET);
    }

    /**
     * Remove password, it requires authenticating first
     * @param pwd password 4 bytes
     * @param pack password acknowledgment 2 bytes
     * @param eventListener @see {@link NTagEventListener}
     */
    public void removePassword(byte[] pwd, byte[] pack, NTagEventListener eventListener) {
        try {
            byte[] response = nfcA.transceive(new byte[]{
                    PWD_AUTH,
                    pwd[0], pwd[1], pwd[2], pwd[3]
            });
            if (response[0] == pack[0] && response[1] == pack[1])
                writeMemoryPage(NO_PAGES, AUTH0_CONFIG_PAGE, eventListener);
            eventListener.OnSuccess(NTagEventListener.ON_PASSWORD_REMOVED, NTagEventListener.PWD_REMOVE);
        } catch (IOException e) {
           if (debugMode) e.printStackTrace();
            eventListener.OnError(e.getMessage(), NTagEventListener.ERROR_AUTHENTICATION_VERIFY);
        }
    }

    /**
     * Parse byte[] to hexadecimal string format
     * @param bytes the array of bytes
     * @return hexadecimal String
     */
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
