package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

public class VcashKeychain {
    private DeterministicKey mMasterKey;

    public VcashKeychain(DeterministicKey key){
        mMasterKey = key;
        Log.d("---------masterkey", key.getPrivateKeyAsHex());
    }

    public byte[] dervieKey(long amount, VcashKeychainPath path){
        DeterministicKey key = this.deriveKey(path);
        byte[] data = NativeSecp256k1.instance().bindSwitch(amount, key.getPrivKeyBytes());
        return data;
    }

    private DeterministicKey deriveKey(VcashKeychainPath path){
        DeterministicKey key = mMasterKey;
        for (int i=0; i<path.mDepth; i++){
            key = HDKeyDerivation.deriveChildKey(key, path.mPath[i]);
        }
        return key;
    }
}
