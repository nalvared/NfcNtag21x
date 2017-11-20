package com.nalvared.ntag21xseries;

import android.nfc.Tag;

/**
 * Created by nestor on 16/11/17.
 */

public class NTag215 extends NTag21x {

    public NTag215(Tag tag) {
        super(tag);
        super.PAGE_USER_START = (byte) 0x04;
        super.PAGE_USER_END   = (byte) 0x81;
        super.AUTH0_CONFIG_PAGE = (byte) 0x83;
        super.ACCESS_CONFIG_PAGE = (byte) 0x84;
        super.PWD_CONFIG_PAGE = (byte) 0x85;
        super.PACK_CONFIG_PAGE = (byte) 0x86;
    }
}
