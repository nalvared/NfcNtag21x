package com.example.nestor.nfctest;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getCanonicalName();

    public static final String MIME_TEXT_PLAIN = "text/plain";

    boolean isWrite = false;

    NfcAdapter mNfcAdapter;

    EditText edCmd;
    Button btnCmd;
    TextView tvResult;

    byte[] command;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        edCmd = findViewById(R.id.edCmd);
        tvResult = findViewById(R.id.tvResult);
        btnCmd = findViewById(R.id.btnCmd);

        btnCmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmd = edCmd.getText().toString();
                command = hexStringToByteArray(cmd);
                Log.i(TAG, Arrays.toString(command));
            }
        });

    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected,tagDetected,ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if(mNfcAdapter!= null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mNfcAdapter!= null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    private static final byte READ = 0x30;
    private static final byte PAGE_41 = 0x29;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        Log.d(TAG, "onNewIntent: "+intent.getAction());

        try {

            byte[] pwd = new byte[] { (byte)0x68, (byte)0x6F, (byte)0x6C, (byte)0x61 };
            byte[] pack = new byte[] { (byte)0x52, (byte)0x52 };

            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            //nTag213.setTimeout(2000);

            byte[] message = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                    "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut " +
                    "enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
                    "aliquip ex ea commodo consequat.").getBytes();
            boolean isCorrect = nTag213.writeToTheLimit(message);

            Log.i(TAG, String.valueOf(isCorrect));
            Log.i(TAG, Arrays.toString(nTag213.getUserMemory()));
            Log.i(TAG, new String(nTag213.getUserMemory()));

            Log.i(TAG, Arrays.toString(nTag213.read()));
            Log.i(TAG, new String(nTag213.read()));
            nTag213.close();



        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void toDefault(NfcA nfcA, byte[] pwd, byte[] pack) throws IOException {
        byte[] response;

        /*response = nfcA.transceive(new byte[]{
                (byte) 0x1B, // PWD_AUTH
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF
        });
        Log.i(TAG+ ".1.1", Arrays.toString(response));*/

        byte[] page = "HOLA".getBytes();
        nfcA.transceive(new byte[] {
                (byte) 0xA2, // WRITE
                (byte) 0x03, // page address
                page[0], page[1], page[2], page[3]
        });

        response = nfcA.transceive(new byte[] {
                0x3A,
                (byte) 0x03,
                0x2C
        });
        Log.i(TAG+ ".1.1", new String(response));
    }

    public void removePwd(NfcA nfcA, byte[] pwd, byte[] pack) throws IOException {
        byte[] response;

        response = nfcA.transceive(new byte[]{
                (byte) 0x1B, // PWD_AUTH
                pwd[0], pwd[1], pwd[2], pwd[3]
        });
        Log.i(TAG+ ".1.1", Arrays.toString(response));

        response = nfcA.transceive(new byte[]{
                (byte) 0x30,
                (byte) 0x2A
        });
        Log.i(TAG+ ".1.2", Arrays.toString(response));

        boolean prot = false;                               // false = PWD_AUTH for write only, true = PWD_AUTH for read and write
        int authlim = 0;                                    // 0 = unlimited tries
        nfcA.transceive(new byte[] {
                (byte) 0xA2, // WRITE
                (byte) 0x2A, // page address
                (byte) ((response[0] & 0x078) | (prot ? 0x080 : 0x000) | (authlim & 0x007)),    // set ACCESS byte according to our settings
                0, 0, 0                                                                         // fill rest as zeros as stated in datasheet (RFUI must be set as 0b)
        });

        int auth0 = 0xFF;                                    // first page to be protected
        nfcA.transceive(new byte[] {
                (byte) 0xA2, // WRITE
                (byte) 0x29, // page address
                4, 0, 0,              // Keep old mirror values and write 0 in RFUI byte as stated in datasheet
                (byte) (auth0 & 0x0ff)
        });

        response = nfcA.transceive(new byte[]{
                (byte) 0x30,
                (byte) 0x2A
        });
        Log.i(TAG+ ".1.3", Arrays.toString(response));
    }

    public void verifyPwd(NfcA nfcA, byte[] pwd, byte[] pack) throws IOException {
        byte[] response;

        //Read page 41 on NTAG213, will be different for other tags
        response = nfcA.transceive(new byte[] {
                (byte) 0x30, // READ
                41           // page address
        });

        Log.i(TAG + ".1", Arrays.toString(response));
        if(response[3] != (byte)0xFF) {
            response = nfcA.transceive(new byte[]{
                    (byte) 0x1B, // PWD_AUTH
                    pwd[0], pwd[1], pwd[2], pwd[3]
            });

            Log.i(TAG+ ".1.1", Arrays.toString(response));
            // Check if PACK is matching expected PACK
            // This is a (not that) secure method to check if tag is genuine
            if ((response != null) && (response.length >= 2)) {
                final byte[] packResponse = Arrays.copyOf(response, 2);
                if (!(pack[0] == packResponse[0] && pack[1] == packResponse[1])) {
                    Log.i(TAG, "No match");
                }
                else {
                    Log.i(TAG, "Match");
                    byte[] bytes = "HOLA".getBytes();
                    Log.i(TAG + ".match", Arrays.toString(bytes));
                    nfcA.transceive(new byte[] {
                            (byte)0xA2,
                            (byte)0x04,
                            bytes[0], bytes[1], bytes[2], bytes[3]
                    });
                    response = nfcA.transceive(new byte[] {
                            (byte) 0x30, // READ
                            (byte) 0x04  // page address
                    });
                    Log.i(TAG + ".match", new String(response));

                    response = nfcA.transceive(new byte[] {
                            (byte) 0x30, // WRITE
                            (byte) 0x29                                                                    // fill rest as zeros as stated in datasheet (RFUI must be set as 0b)
                    });
                    Log.i(TAG + ".match", Arrays.toString(response));

                    // configure tag as write-protected with unlimited authentication tries
                    if ((response != null) && (response.length >= 16)) {    // read always returns 4 pages
                        boolean prot = false;                               // false = PWD_AUTH for write only, true = PWD_AUTH for read and write
                        int authlim = 0;                                    // 0 = unlimited tries
                        nfcA.transceive(new byte[] {
                                (byte) 0xA2, // WRITE
                                (byte) 0x2A, // page address
                                (byte) ((response[0] & 0x078) | (prot ? 0x080 : 0x000) | (authlim & 0x007)),    // set ACCESS byte according to our settings
                                0, 0, 0                                                                         // fill rest as zeros as stated in datasheet (RFUI must be set as 0b)
                        });
                    }
                    // Get page 29h
                    response = nfcA.transceive(new byte[] {
                            (byte) 0x30, // READ
                            (byte) 0x29  // page address
                    });
                    Log.i(TAG + ".3", Arrays.toString(response));

                    // Configure tag to protect entire storage (page 0 and above)
                    if ((response != null) && (response.length >= 16)) {  // read always returns 4 pages
                        int auth0 = 0xFF;                                    // first page to be protected
                        nfcA.transceive(new byte[] {
                                (byte) 0xA2, // WRITE
                                (byte) 0x29, // page address
                                response[0], 0, response[2],              // Keep old mirror values and write 0 in RFUI byte as stated in datasheet
                                (byte) (auth0 & 0x0ff)
                        });
                    }
                    // Get page 29h
                    response = nfcA.transceive(new byte[] {
                            (byte) 0x30, // READ
                            (byte) 0x29  // page address
                    });
                    Log.i(TAG + ".3", Arrays.toString(response));
                }
            }

        }
        else {
            // Protect tag with your password in case
            // it's not protected yet

            // Get Page 2Ah
            response = nfcA.transceive(new byte[] {
                    (byte) 0x30, // READ
                    (byte) 0x2A  // page address
            });
            Log.i(TAG + ".2", Arrays.toString(response));

            // configure tag as write-protected with unlimited authentication tries
            if ((response != null) && (response.length >= 16)) {    // read always returns 4 pages
                boolean prot = false;                               // false = PWD_AUTH for write only, true = PWD_AUTH for read and write
                int authlim = 0;                                    // 0 = unlimited tries
                nfcA.transceive(new byte[] {
                        (byte) 0xA2, // WRITE
                        (byte) 0x2A, // page address
                        (byte) ((response[0] & 0x078) | (prot ? 0x080 : 0x000) | (authlim & 0x007)),    // set ACCESS byte according to our settings
                        0, 0, 0                                                                         // fill rest as zeros as stated in datasheet (RFUI must be set as 0b)
                });
            }
            // Get page 29h
            response = nfcA.transceive(new byte[] {
                    (byte) 0x30, // READ
                    (byte) 0x29  // page address
            });
            Log.i(TAG + ".3", Arrays.toString(response));

            // Configure tag to protect entire storage (page 0 and above)
            if ((response != null) && (response.length >= 16)) {  // read always returns 4 pages
                int auth0 = 0;                                    // first page to be protected
                nfcA.transceive(new byte[] {
                        (byte) 0xA2, // WRITE
                        (byte) 0x29, // page address
                        response[0], 0, response[2],              // Keep old mirror values and write 0 in RFUI byte as stated in datasheet
                        (byte) (auth0 & 0x0ff)
                });
            }

            // Send PACK and PWD
            // set PACK:
            nfcA.transceive(new byte[] {
                    (byte)0xA2,
                    (byte)0x2C,
                    pack[0], pack[1], 0, 0  // Write PACK into first 2 Bytes and 0 in RFUI bytes
            });
            // set PWD:
            nfcA.transceive(new byte[] {
                    (byte)0xA2,
                    (byte)0x2B,
                    pwd[0], pwd[1], pwd[2], pwd[3] // Write all 4 PWD bytes into Page 43
            });

        }
    }
    /*private void readFromNFC(Ndef ndef) {

        try {
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            String message = new String(ndefMessage.getRecords()[0].getPayload());
            Log.d(TAG, "readFromNFC: "+message);

            Log.i(TAG, (Arrays.toString(ndef.getNdefMessage().toByteArray())));

            for (NdefRecord record: ndef.getNdefMessage().getRecords()){
                Log.d(TAG, new String(record.getType()));
                Log.d(TAG, new String(record.getPayload()));
            }

            edRead.setText(message);
            ndef.close();

        } catch (IOException | FormatException e) {
            e.printStackTrace();

        }
    }*/

    /*@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void writeToNfc(Ndef ndef, String message){

        if (ndef != null) {

            try {
                ndef.connect();
                NdefRecord mimeRecord = NdefRecord.createMime("text/plain",
                        message.getBytes(Charset.forName("US-ASCII")));
                ndef.writeNdefMessage(new NdefMessage(mimeRecord));
                ndef.close();
                //Write Successful

            } catch (IOException | FormatException e) {
                e.printStackTrace();


            } finally {

            }

        }
    }*/
}
