package com.vcashorg.vcashwallet;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.vcashorg.vcashwallet.wallet.WalletApi;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WalletApi.setWalletContext(getApplicationContext());
        WalletApi.createWallet(null, null);
        Log.d("----------------", WalletApi.getWalletUserId());
    }
}
