package com.nalvared.ntag21xseries;

import android.nfc.Tag;

/**
 * Created by nestor on 16/11/17.
 */

public class NTag213 extends NTag21x {

    public NTag213(Tag tag) {
        super(tag);
        super.PAGE_USER_START = (byte) 0x04;
        super.PAGE_USER_END   = (byte) 0x27;
        super.AUTH0_CONFIG_PAGE = (byte) 0x29;
        super.ACCESS_CONFIG_PAGE = (byte) 0x2A;
        super.PWD_CONFIG_PAGE = (byte) 0x2B;
        super.PACK_CONFIG_PAGE = (byte) 0x2C;
    }
}
