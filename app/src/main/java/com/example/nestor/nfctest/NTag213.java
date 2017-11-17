package com.example.nestor.nfctest;

import android.nfc.Tag;

/**
 * Created by nestor on 16/11/17.
 */

public class NTag213 extends NTag21x implements NTag21xException{

    public NTag213(Tag tag) {
        super(tag);
        super.PAGE_USER_START = (byte) 0x04;
        super.PAGE_USER_END   = (byte) 0x27;
        super.AUTH0_CONFIG_PAGE = (byte) 0x29;
        super.ACCESS_CONFIG_PAGE = (byte) 0x2A;
        super.PWD_CONFIG_PAGE = (byte) 0x2B;
        super.PACK_CONFIG_PAGE = (byte) 0x2C;

        super.setInterfaceException(this);
    }

    private static final String E_USER_PAGE_LIMIT = "Writing %d bytes and the Tag has only 144 bytes";
    private static final String E_TIMEOUT = "Tag must be connected (Check timeout for large operations)";

    @Override
    public void isNotConnected() throws Exception {
        throw new Exception(E_TIMEOUT);
    }

    @Override
    public void pageUserExceeded(int max) throws Exception {
        throw new Exception(String.format(E_USER_PAGE_LIMIT, max));
    }
}
