package com.nalvared.ntag21xseries;

/**
 * Created by nestor on 21/11/17.
 */

public interface NTag21xEventListener {

    void OnSuccess(Object result);
    void OnError(String error, int code);
}
