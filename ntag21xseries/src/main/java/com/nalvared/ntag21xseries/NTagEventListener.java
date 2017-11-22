package com.nalvared.ntag21xseries;

/**
 * Created by nestor on 21/11/17.
 */

public interface NTagEventListener {

    void OnSuccess(Object response, int code);
    void OnError(String error, int code);

    // Success codes
    int SC_READ_UID = 0;
    int SC_READ = 1;
    int SC_WRITE = 2;
    int SC_PWD = 3;

    // Error codes
    int ERROR_TRANSCEIVE = 0;
    int ERROR_MAX_CAPACITY = 1;
    int ERROR_AUTHENTICATION_VERIFY = 2;

    // Error messages
    String ERROR_MAX_CAPACITY_MSG = "The length of message exceeded the tag capacity";
    String ERROR_PWD = "Impossible to check password";

    // Info messages
    String ON_WRITE_SUCCESS = "The writing operation has been completed successfully";
    String ON_PASSWORD_ASSIGN = "Password successfully assigned";
    String ON_PASSWORD_REMOVED = "Password successfully removed";
    String NO_PWD = "It does not have password";
    String WRITE_ONLY_PWD = "Password is required for writing";
    String RW_PWD = "Password is required both read and write";


}
