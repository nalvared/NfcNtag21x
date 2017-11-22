package com.nalvared.ntag21xseries;

/**
 * <H1>NTag21x</H1>
 *
 * <P>This interface provide two method for interacting with the {@link NTag21x}</P>
 *
 * <P>Néstor Álvarez Díaz, contact[at]nalvared.com</P>
 *
 * @author Néstor Álvarez Díaz
 * @date 2017/11/16
 * @version 1.0.1
 *
 */

public interface NTagEventListener {

    /**
     * This method is always called when a function ends as expected
     * @param response - It is a byte[] or String depending of the called function
     *                 Object can be cast as following when is used the class {@link NTag21x}:
     *                 <ul>
     *                  <li>Cast Object to byte[]
     *                      <ul>
     *                          <li>{@link NTag21x#getStaticId(int, NTagEventListener)} with {@link NTag21x#UID_BYTES} flag</li>
     *                          <li>{@link NTag21x#getUserMemory(NTagEventListener)}: (byte[]) response</li>
     *                          <li>{@link NTag21x#read(NTagEventListener)}: (byte[]) response</li>
     *                      </ul>
     *                 </li>
     *                 <li>Cast Object to String
     *                  <ul>
     *                        <li>{@link NTag21x#getStaticId(int, NTagEventListener)} with {@link NTag21x#UID_SRTING}</li>
     *                        <li>{@link NTag21x#write(byte[], NTagEventListener)}</li>
     *                        <li>{@link NTag21x#writeAndTrucate(byte[], NTagEventListener)}</li>
     *                        <li>{@link NTag21x#authAndWrite(byte[], byte[], byte[], NTagEventListener)}</li>
     *                        <li>{@link NTag21x#hasPassword(NTagEventListener)}</li>
     *                        <li>{@link NTag21x#setPassword(byte[], byte[], int, NTagEventListener)}</li>
     *                        <li>{@link NTag21x#removePassword(byte[], byte[], NTagEventListener)}</li>
     *                  </ul>
     *                 </li>
     *                 </ul>
     * @param code - Identifier response code
     */
    void OnSuccess(Object response, int code);

    /**
     * When a function catch an error, this method is called
     * @param error - try - catch error message
     * @param code - Identifier of error
     */
    void OnError(String error, int code);

    // Success codes
    int READ_STATIC_ID_BYTES = 0;
    int READ_STATIC_ID_STRING = 1;
    int READ_USER_MEMORY = 2;
    int READ = 3;
    int WRITE = 4;
    int PWD_SET = 5;
    int PWD_REMOVE = 6;

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
