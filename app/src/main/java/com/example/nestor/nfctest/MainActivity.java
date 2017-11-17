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
            byte[] pack = new byte[] { (byte)0x52, (byte)0x52, (byte) 0x00, (byte) 0x00 };

            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            //nTag213.setPassword(pwd, pack, NTag213.FLAG_ONLY_WRITE);
            nTag213.removePassword(pwd,pack);
            int protection = nTag213.needAuthentication();
            Log.i(TAG, String.valueOf(protection));
            nTag213.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void print(byte[] bytes) {
        Log.i(TAG, Arrays.toString(bytes));
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
