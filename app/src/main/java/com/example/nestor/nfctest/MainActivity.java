package com.example.nestor.nfctest;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
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

    int status = 0;

    NfcAdapter mNfcAdapter;

    EditText edMessage;
    EditText edPwd;

    TextView tvResult;
    TextView tvWait;

    LinearLayout lyWait;

    Button btnRead;
    Button btnWrite;
    Button btnSetPwd;
    Button btnDelPwd;
    Button btnAuthAndWrite;
    Button btnHasPwd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        edMessage = findViewById(R.id.edMessage);
        edPwd = findViewById(R.id.edPwd);
        tvResult = findViewById(R.id.tvResult);
        btnRead = findViewById(R.id.btnRead);
        btnWrite = findViewById(R.id.btnWrite);
        btnSetPwd = findViewById(R.id.btnAddPwd);
        btnDelPwd = findViewById(R.id.btnDelPwd);
        btnAuthAndWrite = findViewById(R.id.btnAuthAndWrite);
        btnHasPwd = findViewById(R.id.btnHasPwd);
        lyWait = findViewById(R.id.lyWait);
        tvWait = findViewById(R.id.tvWait);

        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                status = 0;
                tvWait.setText("Approach the Tag to\nread it");
                lyWait.setVisibility(View.VISIBLE);
            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                status = 1;
                tvWait.setText("Approach the Tag to\nwrite it");
                lyWait.setVisibility(View.VISIBLE);
            }
        });

        btnSetPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                status = 2;
                tvWait.setText("Approach the Tag to\nset password");
                lyWait.setVisibility(View.VISIBLE);
            }
        });

        btnDelPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                status = 3;
                tvWait.setText("Approach the Tag to\ndelete password");
                lyWait.setVisibility(View.VISIBLE);
            }
        });

        btnAuthAndWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                status = 4;
                tvWait.setText("Approach the Tag to\nwrite with authentication");
                lyWait.setVisibility(View.VISIBLE);
            }
        });

        btnHasPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                status = 5;
                tvWait.setText("Approach the Tag to\ncheck if it has password");
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

    private void hideKeyBoard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if(tag != null) {
            switch (status) {
                case 0:
                    tvResult.setText(readFromNFC(tag));
                    break;
                case 1:
                    String message = edMessage.getText().toString();
                    String r = writeToNfc(tag, message.getBytes());
                    if (r != null)
                        tvResult.setText(r);
                    else
                        tvResult.setText("Error while writing");
                    break;
                case 2:
                    String pwd = edPwd.getText().toString();
                    r = setPwd(tag, pwd.getBytes());
                    if (r != null)
                    tvResult.setText(r);
                else
                    tvResult.setText("Error while setting password");
                    break;
                case 3:
                    pwd = edPwd.getText().toString();
                    r = removePwd(tag, pwd.getBytes());
                    if (r != null)
                        tvResult.setText(r);
                    else
                        tvResult.setText("Error while removing password");
                    break;
                case 4:
                    message = edMessage.getText().toString();
                    pwd = edPwd.getText().toString();
                    r = authAndWrite(tag, pwd.getBytes(), message.getBytes());
                    if (r != null)
                        tvResult.setText(r);
                    else
                        tvResult.setText("Error while writing");
                    break;
                case 5:
                    int hasPwd = hasPwd(tag);
                    tvResult.setText(hasPwdMessage(hasPwd));
                    break;
            }
        }
        lyWait.setVisibility(View.GONE);
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
        }
        return null;
    }

    private String writeToNfc(Tag tag, byte[] message){
        try {
            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            nTag213.write(message);
            nTag213.close();
            return "Write successful";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String setPwd(Tag tag, byte[] pwd){
        try {
            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            nTag213.setPassword(
                    new byte[]{
                        pwd[0], pwd[1], pwd[2], pwd[3]
                    },
                    new byte[]{
                        pwd[4], pwd[5], 0x00, 0x00
                    },
                    NTag21x.FLAG_ONLY_WRITE
            );
            nTag213.close();
            return "Password has been updated successful";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String removePwd(Tag tag, byte[] pwd){
        try {
            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            nTag213.removePassword(
                    new byte[]{
                            pwd[0], pwd[1], pwd[2], pwd[3]
                    },
                    new byte[]{
                            pwd[4], pwd[5], 0x00, 0x00
                    }
            );
            nTag213.close();
            return "Password has been removed successful";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String authAndWrite(Tag tag, byte[] pwd, byte[] message){
        try {
            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            nTag213.authAndWrite(
                    new byte[]{
                            pwd[0], pwd[1], pwd[2], pwd[3]
                    },
                    new byte[]{
                            pwd[4], pwd[5], 0x00, 0x00
                    },
                    message
            );
            nTag213.close();
            return "Write successful";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int hasPwd(Tag tag){
        try {
            NTag213 nTag213 = new NTag213(tag);
            nTag213.connect();
            int i = nTag213.needAuthentication();
            nTag213.close();
            return i;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -2;
    }

    private String hasPwdMessage(int i) {
        switch (i) {
            case -2:
                return "[-2] no identifiable";
            case -1:
                return "[-1] the tag is not protected";
            case 0:
                return "[0] the tag is protected against write";
            case 1:
                return "[1] the tag is protected both read and write";
            default:
                return "[X] no identifiable";
        }
    }
}