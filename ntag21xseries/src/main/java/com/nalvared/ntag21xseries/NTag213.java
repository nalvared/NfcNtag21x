package com.nalvared.ntag21xseries;

import android.nfc.Tag;

/**
 * <H1>NTag213 extends NTag21x</H1>
 *
 * <P>This class defines the main pages for NTag213 specific model</P>
 *
 * <P>Néstor Álvarez Díaz, contact[at]nalvared.com</P>
 *
 * @author Néstor Álvarez Díaz
 * @version 1.0.0
 * @date 2017-11-16
 *
 */
public class NTag213 extends NTag21x {

    /**
     * Constructor
     * @param tag is a raw Tag object
     */
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
