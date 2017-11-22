package com.nalvared.ntag21xseries;

import android.nfc.Tag;

/**
 * <H1>NTag215 extends NTag21x</H1>
 *
 * <P>This class defines the main pages for NTag215 specific model</P>
 *
 * <P>Néstor Álvarez Díaz, contact[at]nalvared.com</P>
 *
 * @author Néstor Álvarez Díaz
 * @version 1.0.0
 * @date 2017-11-16
 *
 */
public class NTag215 extends NTag21x {

    /**
     * Constructor
     * @param tag is a raw Tag object
     */
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
