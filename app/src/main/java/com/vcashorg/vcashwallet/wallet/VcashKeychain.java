package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashProofInfo;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import java.nio.ByteBuffer;

public class VcashKeychain {
    private DeterministicKey mMasterKey;

    public VcashKeychain(DeterministicKey key){
        mMasterKey = key;
    }

    public byte[] deriveBindKey(long amount, VcashKeychainPath path, SwitchCommitmentType commitmentType){
        DeterministicKey key = this.deriveKey(path);
        if (commitmentType == SwitchCommitmentType.SwitchCommitmentTypeRegular){
            byte[] data = NativeSecp256k1.instance().bindSwitch(amount, key.getPrivKeyBytes());
            return data;
        }
        else if(commitmentType == SwitchCommitmentType.SwitchCommitmentTypeNone){
            return key.getPrivKeyBytes();
        }
        else {
            return null;
        }
    }

    public byte[] createCommitment(long amount, VcashKeychainPath path, SwitchCommitmentType commitmentType) {
        byte[] key = this.deriveBindKey(amount, path, commitmentType);
        byte[] data = NativeSecp256k1.instance().getCommitment(amount, key);
        return data;
    }

    public byte[] createNonce(byte[] commitment){
        byte[] rootKey = this.deriveBindKey(0, new VcashKeychainPath(0, 0, 0, 0,0), SwitchCommitmentType.SwitchCommitmentTypeRegular);
        byte[] nounceData = NativeSecp256k1.instance().blake2b(rootKey, commitment);
        return nounceData;
    }

    public byte[] createNonceV2(byte[] commitment, boolean forPrivate){
        byte[] rootKey = this.deriveBindKey(0, new VcashKeychainPath(0, 0, 0, 0,0), SwitchCommitmentType.SwitchCommitmentTypeNone);
        byte[] keyHashData;
        if (forPrivate){
            keyHashData = NativeSecp256k1.instance().blake2b(rootKey, null);
        }
        else{
            byte[] pubKey = NativeSecp256k1.instance().getPubkeyFromSecretKey(rootKey);
            byte[] compressedPubkey = NativeSecp256k1.instance().getCompressedPubkey(pubKey);
            keyHashData = NativeSecp256k1.instance().blake2b(compressedPubkey, null);
        }
        byte[] nounceData = NativeSecp256k1.instance().blake2b(keyHashData, commitment);
        return nounceData;
    }

    public byte[] createRangeProof(long amount, VcashKeychainPath path){
        byte[] commit = this.createCommitment(amount, path, SwitchCommitmentType.SwitchCommitmentTypeRegular);
        byte[] secketKey = this.deriveBindKey(amount, path, SwitchCommitmentType.SwitchCommitmentTypeRegular);
        byte[] rewindNounce = this.createNonceV2(commit, false);
        byte[] privateNounce = this.createNonceV2(commit, true);
        ByteBuffer buf = ByteBuffer.allocate(20);
        buf.put((byte) 0);
        buf.put((byte)0);
        buf.put((byte)SwitchCommitmentType.SwitchCommitmentTypeRegular.ordinal());
        buf.put((byte)3);
        buf.put(path.pathData());
        byte[] rangeProof = NativeSecp256k1.instance().createbulletProof(amount, secketKey, rewindNounce, privateNounce, buf.array());
        return rangeProof;
    }

    public VcashProofInfo rewindProof(byte[] commit, byte[] proof){
        byte[] nounce = this.createNonce(commit);
        VcashProofInfo proofInfo = NativeSecp256k1.instance().rewindBulletProof(commit, nounce, proof);
        if (proofInfo == null){
            byte[] rewindNounce = this.createNonceV2(commit, false);
            proofInfo = NativeSecp256k1.instance().rewindBulletProof(commit, rewindNounce, proof);
            if (proofInfo != null){
                proofInfo.version = 1;
            }
        }
        return proofInfo;
    }

    public DeterministicKey deriveKey(VcashKeychainPath path){
        DeterministicKey key = mMasterKey;
        for (int i=0; i<path.mDepth; i++){
            key = HDKeyDerivation.deriveChildKey(key, path.mPath[i]);
        }
        return key;
    }

    public enum SwitchCommitmentType{
        SwitchCommitmentTypeNone,
        SwitchCommitmentTypeRegular,
    }
}
