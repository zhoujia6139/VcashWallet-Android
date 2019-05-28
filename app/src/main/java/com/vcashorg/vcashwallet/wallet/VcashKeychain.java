package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import com.vcashorg.vcashwallet.wallet.WallegtType.VcashProofInfo;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

public class VcashKeychain {
    private DeterministicKey mMasterKey;

    public VcashKeychain(DeterministicKey key){
        mMasterKey = key;
        Log.d("---------masterkey", key.getPrivateKeyAsHex());
    }

    public byte[] deriveBindKey(long amount, VcashKeychainPath path){
        DeterministicKey key = this.deriveKey(path);
        byte[] data = NativeSecp256k1.instance().bindSwitch(amount, key.getPrivKeyBytes());
        return data;
    }

    public byte[] createCommitment(long amount, VcashKeychainPath path) {
        DeterministicKey key = this.deriveKey(path);
        byte[] data = NativeSecp256k1.instance().getCommitment(amount, key.getPrivKeyBytes());
        return data;
    }

    public byte[] createNonce(byte[] commitment){
        byte[] rootKey = this.deriveBindKey(0, new VcashKeychainPath(0, 0, 0, 0,0));
        byte[] nounceData = NativeSecp256k1.instance().blake2b(rootKey, commitment);
        return nounceData;
    }

    public byte[] createRangeProof(long amount, VcashKeychainPath path){
        byte[] commit = this.createCommitment(amount, path);
        byte[] secketKey = this.deriveBindKey(amount, path);
        byte[] nounce = this.createNonce(commit);
        byte[] rangeProof = NativeSecp256k1.instance().createbulletProof(amount, secketKey, nounce, path.pathData());
        return rangeProof;
    }

    public boolean verifyProof(byte[] commit, byte[] proof){
        return NativeSecp256k1.instance().verifyBulletProof(commit, proof);
    }

    public VcashProofInfo rewindProof(byte[] commit, byte[] proof){
        byte[] nounce = this.createNonce(commit);
        return NativeSecp256k1.instance().rewindBulletProof(commit, nounce, proof);
    }

    private DeterministicKey deriveKey(VcashKeychainPath path){
        DeterministicKey key = mMasterKey;
        for (int i=0; i<path.mDepth; i++){
            key = HDKeyDerivation.deriveChildKey(key, path.mPath[i]);
        }
        return key;
    }
}
