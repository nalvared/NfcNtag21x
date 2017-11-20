package com.nalvared.ntag21xseries;

/**
 * Created by nestor on 17/11/17.
 */

public interface NTag21xException {
    void isNotConnected() throws Exception;
    void pageUserExceeded(int max) throws Exception;
}
