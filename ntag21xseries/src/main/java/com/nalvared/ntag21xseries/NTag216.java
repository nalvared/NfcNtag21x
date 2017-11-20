package com.nalvared.ntag21xseries;

import android.nfc.Tag;

/**
 * Created by nestor on 16/11/17.
 */

public class NTag216 extends NTag21x {

    public NTag216(Tag tag) {
        super(tag);
        super.PAGE_USER_START = (byte) 0x04;
        super.PAGE_USER_END   = (byte) 0xE1;
        super.AUTH0_CONFIG_PAGE = (byte) 0xE3;
        super.ACCESS_CONFIG_PAGE = (byte) 0xE4;
        super.PWD_CONFIG_PAGE = (byte) 0xE5;
        super.PACK_CONFIG_PAGE = (byte) 0xE6;
    }
}
