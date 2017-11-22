package com.example.nestor.nfctest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nalvared.ntag21xseries.NTag213;
import com.nalvared.ntag21xseries.NTag21x;
import com.nalvared.ntag21xseries.NTagEventListener;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getCanonicalName();

    int status = 0;

    NfcAdapter mNfcAdapter;

    EditText edMessage;
    EditText edPwd;

    TextView tvResult;
    TextView tvWait;

    LinearLayout lyWait;

    Button btnRead;
    Button btnWrite;
    Button btnUid;
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
        btnUid = findViewById(R.id.btnUid);
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

        btnUid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                status = 6;
                tvWait.setText("Approach the Tag to\nget the static ID");
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
                    read(tag);
                    break;
                case 1:
                    String message = edMessage.getText().toString();
                    write(tag, message);
                    break;
                case 2:
                    String pwd = edPwd.getText().toString();
                    setPwd(tag, pwd);
                    break;
                case 3:
                    pwd = edPwd.getText().toString();
                    removePwd(tag, pwd);
                    break;
                case 4:
                    message = edMessage.getText().toString();
                    pwd = edPwd.getText().toString();
                    authAndWrite(tag, pwd, message);
                    break;
                case 5:
                    hasPwd(tag);
                    break;
                case 6:
                    uid(tag);
                    break;
            }
        }
        lyWait.setVisibility(View.GONE);
    }

    private void uid(Tag tag) {
        NTag213 nTag213 = new NTag213(tag);
        nTag213.connect();
        nTag213.getStaticId(NTag21x.UID_SRTING, new NTagEventListener() {
            @Override
            public void OnSuccess(Object response, int code) {
                tvResult.setText((String) response);
            }

            @Override
            public void OnError(String error, int code) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        nTag213.close();
    }

    private void read(Tag tag) {
        NTag213 nTag213 = new NTag213(tag);
        nTag213.connect();
        nTag213.read(new NTagEventListener() {
            @Override
            public void OnSuccess(Object response, int code) {
                byte[] r = (byte[]) response;
                tvResult.setText(new String(r));
            }

            @Override
            public void OnError(String error, int code) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        nTag213.close();
    }

    private void write(Tag tag, String message){
        NTag213 nTag213 = new NTag213(tag);
        nTag213.connect();
        nTag213.write(message.getBytes(), new NTagEventListener() {
            @Override
            public void OnSuccess(Object response, int code) {
                Toast.makeText(getApplicationContext(), (String) response, Toast.LENGTH_LONG).show();
            }

            @Override
            public void OnError(String error, int code) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        nTag213.close();
    }

    private void setPwd(Tag tag, String password){
        if (password.length() != 6){
            Toast.makeText(getApplicationContext(), "Password length error", Toast.LENGTH_LONG).show();
            return;
        }
        byte[] pwd = Arrays.copyOfRange(password.getBytes(), 0, 4);
        byte[] pack = Arrays.copyOfRange(password.getBytes(), 4, 6);

        NTag213 nTag213 = new NTag213(tag);
        nTag213.connect();
        nTag213.setPassword(pwd, pack, NTag213.FLAG_ONLY_WRITE, new NTagEventListener() {
            @Override
            public void OnSuccess(Object response, int code) {
                Toast.makeText(getApplicationContext(), (String) response, Toast.LENGTH_LONG).show();
            }

            @Override
            public void OnError(String error, int code) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        nTag213.close();
    }

    private void removePwd(Tag tag, String password){

        Log.i(TAG, password);
        if (password.length() != 6){
            Toast.makeText(getApplicationContext(), "Password length error", Toast.LENGTH_LONG).show();
            return;
        }
        byte[] pwd = Arrays.copyOfRange(password.getBytes(), 0, 4);
        byte[] pack = Arrays.copyOfRange(password.getBytes(), 4, 6);


        Log.i(TAG, Arrays.toString(pwd));
        Log.i(TAG, Arrays.toString(pack));

        NTag213 nTag213 = new NTag213(tag);
        nTag213.connect();
        nTag213.removePassword(pwd, pack, new NTagEventListener() {
            @Override
            public void OnSuccess(Object response, int code) {
                Toast.makeText(getApplicationContext(), (String) response, Toast.LENGTH_LONG).show();
            }

            @Override
            public void OnError(String error, int code) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        nTag213.close();
    }

    private void authAndWrite(Tag tag, String password, String message){
        byte[] pwd = Arrays.copyOfRange(password.getBytes(), 0, 4);
        byte[] pack = Arrays.copyOfRange(password.getBytes(), 4, 6);
        NTag213 nTag213 = new NTag213(tag);
        nTag213.connect();
        nTag213.authAndWrite(pwd, pack, message.getBytes(), new NTagEventListener() {
            @Override
            public void OnSuccess(Object response, int code) {
                Toast.makeText(getApplicationContext(), (String) response, Toast.LENGTH_LONG).show();
            }

            @Override
            public void OnError(String error, int code) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        nTag213.close();
    }

    private void hasPwd(Tag tag){
        NTag213 nTag213 = new NTag213(tag);
        nTag213.connect();
        nTag213.hasPassword(new NTagEventListener() {
            @Override
            public void OnSuccess(Object response, int code) {
                Toast.makeText(getApplicationContext(), (String) response, Toast.LENGTH_LONG).show();
            }

            @Override
            public void OnError(String error, int code) {
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            }
        });
        nTag213.close();
    }
}