package com.example.nestor.nfctest;

import android.annotation.TargetApi;
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
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getCanonicalName();

    public static final String MIME_TEXT_PLAIN = "text/plain";

    boolean isWrite = false;

    NfcAdapter mNfcAdapter;

    EditText edMessage;
    TextView tvResult;
    TextView tvWait;
    LinearLayout lyWait;

    private Button btnRead;
    private Button btnWrite;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        edMessage = findViewById(R.id.edMessage);
        tvResult = findViewById(R.id.tvResult);
        btnRead = findViewById(R.id.btnRead);
        btnWrite = findViewById(R.id.btnWrite);
        lyWait = findViewById(R.id.lyWait);
        tvWait = findViewById(R.id.tvWait);

        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWrite = false;
                tvWait.setText("Approach the Tag to READ it");
                lyWait.setVisibility(View.VISIBLE);
            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWrite = true;
                tvWait.setText("Approach the Tag to WRITE it");
                lyWait.setVisibility(View.VISIBLE);
            }
        });

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if(tag != null && !isWrite) {
            tvResult.setText(readFromNFC(tag));
        }
        else if(tag != null && isWrite) {
            String message = edMessage.getText().toString();
            writeToNfc(tag, message);
        }
    }

    private String readFromNFC(Tag tag) {

        try {
            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            byte[] response = nTag213.read();
            nTag213.close();
            return new String(response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lyWait.setVisibility(View.GONE);
        }
        return null;
    }

    private void writeToNfc(Tag tag, String message){
        try {
            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            nTag213.write(message.getBytes());
            nTag213.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lyWait.setVisibility(View.GONE);
        }
    }
}
