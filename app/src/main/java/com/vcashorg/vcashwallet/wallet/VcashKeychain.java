package com.vcashorg.vcashwallet.wallet;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

public class VcashKeychain {
    private DeterministicKey mMasterKey;

    public VcashKeychain(DeterministicKey key){
        mMasterKey = key;
    }

    private DeterministicKey deriveKey(VcashKeychainPath path){
        DeterministicKey key = mMasterKey;
        for (int i=0; i<path.mDepth; i++){
            key = HDKeyDerivation.deriveChildKey(key, path.mPath[i]);
        }
        return key;
    }
}
