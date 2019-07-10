package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashProofInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class NativeSecp256k1 {
    static {
        System.loadLibrary("secp256k1_wrapper");
    }
    private static NativeSecp256k1 sIntance;
    private long mContext;

    public static NativeSecp256k1 instance() {
        if (sIntance == null) {
            sIntance = new NativeSecp256k1();
        }
        return sIntance;
    }

    private NativeSecp256k1(){
        mContext = this.secp256k1_ctx_create();
    }

    public byte[] bindSwitch(long value, byte[] key){
        return this.secp256k1_bind_switch(mContext, value, key);
    }

    public boolean verifyEcSecretKey(byte[] data){
        return this.secp256k1_verify_ec_ecretKey(mContext, data);
    }

    public byte[] getCommitment(long value, byte[] key){
        return this.secp256k1_get_commitment(mContext, value, key);
    }

    public byte[] bindSum(ArrayList<byte[]> positive, ArrayList<byte[]> negative){

        return this.secp256k1_bind_sum(mContext, AppUtil.ArrayListToByteArr(positive), AppUtil.ArrayListToByteArr(negative));
    }

    public byte[] commitSum(ArrayList<byte[]> positive, ArrayList<byte[]> negative){
        return this.secp256k1_commit_sum(mContext, AppUtil.ArrayListToByteArr(positive), AppUtil.ArrayListToByteArr(negative));
    }

    public byte[] commitToPubkey(byte[] commit){
        return this.secp256k1_commit_to_pubkey(mContext, commit);
    }

    public byte[] getCompressedPubkey(byte[] pubkey){
        if (pubkey == null){
            return null;
        }
        return this.secp256k1_get_compressed_pubkey(mContext, pubkey);
    }

    public byte[] pubkeyFromCompressedKey(byte[] compressedKey) {
        if (compressedKey == null){
            return null;
        }
        return this.secp256k1_pubkey_from_compressed_key(mContext, compressedKey);
    }

    public boolean verifySingleSignature(byte[] signature, byte[] pubkey, byte[] nounceSum, byte[] pubkeySum, byte[] msg){
        return this.secp256k1_verify_single_signature(mContext, signature, pubkey, nounceSum, pubkeySum, msg);
    }

    public byte[] calculateSingleSignature(byte[] secKey, byte[] secNounce, byte[] nounceSum, byte[] pubkeySum, byte[] msg){
        return this.secp256k1_calculate_single_signature(mContext, secKey, secNounce, nounceSum, pubkeySum, msg);
    }

    public byte[] combinationPubkey(ArrayList<byte[]> pubkeyArr){
        return this.secp256k1_combination_pubkey(mContext, AppUtil.ArrayListToByteArr(pubkeyArr));
    }

    public byte[] combinationSignatureAndNonceSum(ArrayList<byte[]> sigArr, byte[] nonceSum){
        return this.secp256k1_combination_signature_and_nonceSum(mContext, AppUtil.ArrayListToByteArr(sigArr), nonceSum);
    }

    public byte[] signatureToCompactData(byte[] signature){
        if (signature == null){
            return null;
        }
        return this.secp256k1_signature_to_compactData(mContext, signature);
    }

    public byte[] compactDataToSignature(byte[] compaceData){
        if (compaceData == null){
            return null;
        }
        return this.secp256k1_compact_data_to_signature(mContext, compaceData);
    }

    public byte[] exportSecnonceSingle(){
        return this.secp256k1_export_secnonce_single(mContext);
    }

    public byte[] getPubkeyFromSecretKey(byte[] secKey){
        return this.secp256k1_get_pubkey_from_secretKey(mContext, secKey);
    }

    public byte[] createbulletProof(long value, byte[] secKey, byte[] nounce, byte[] msg){
        return this.secp256k1_createbullet_proof(mContext, value, secKey, nounce, msg);
    }

    public boolean verifyBulletProof(byte[] commitment, byte[] proof){
        return this.secp256k1_verify_bullet_proof(mContext, commitment, proof);
    }

    public VcashProofInfo rewindBulletProof(byte[] commitment, byte[] nounce, byte[] proof){
        VcashProofInfo retData = this.secp256k1_rewind_bullet_proof(mContext, commitment, nounce, proof);
        return retData;
    }

    public byte[] ecdsaSign(byte[] msgdata, byte[] seckey){
        if (msgdata != null && seckey != null){
            return this.secp256k1_ecdsa_sign(mContext, msgdata, seckey);
        }
        return null;
    }

    public boolean ecdsaVerify(byte[] msgdata, byte[] sigData, byte[] pubkey){
        if (msgdata != null && sigData != null && pubkey != null){
            return this.secp256k1_ecdsa_verify(mContext, msgdata, sigData, pubkey);
        }
        return false;
    }

    public byte[] blake2b(byte[] inputData, byte[] key){
        return this.blake_2b(inputData, key);
    }

    private native long secp256k1_ctx_create();

    private native byte[] secp256k1_bind_switch(long context, long value, byte[] key);

    private native boolean secp256k1_verify_ec_ecretKey(long context, byte[] data);

    private native byte[] secp256k1_get_commitment(long context, long value, byte[] key);

    private native byte[] secp256k1_bind_sum(long context, byte[][] positive, byte[][] negative);

    private native byte[] secp256k1_commit_sum(long context, byte[][] positive, byte[][] negative);

    private native byte[] secp256k1_commit_to_pubkey(long context, byte[] commit);

    private native byte[] secp256k1_get_compressed_pubkey(long context, byte[] pubkey);

    private native byte[] secp256k1_pubkey_from_compressed_key(long context, byte[] compressedKey);

    private native boolean secp256k1_verify_single_signature(long context, byte[] signature, byte[] pubkey, byte[] nounceSum, byte[] pubkeySum, byte[] msg);

    private native byte[] secp256k1_calculate_single_signature(long context, byte[] secKey, byte[] secNounce, byte[] nounceSum, byte[] pubkeySum, byte[] msg);

    private native byte[] secp256k1_combination_pubkey(long context, byte[][] pubkeyArr);

    private native byte[] secp256k1_combination_signature_and_nonceSum(long context, byte[][] sigArr, byte[] nonceSum);

    private native byte[] secp256k1_signature_to_compactData(long context, byte[] signature);

    private native byte[] secp256k1_compact_data_to_signature(long context, byte[] compaceData);

    private native byte[] secp256k1_export_secnonce_single(long context);

    private native byte[] secp256k1_get_pubkey_from_secretKey(long context, byte[] secKey);

    private native byte[] secp256k1_createbullet_proof(long context, long value, byte[] secKey, byte[] nounce, byte[] msg);

    private native boolean secp256k1_verify_bullet_proof(long context, byte[] commitment, byte[] proof);

    private native VcashProofInfo secp256k1_rewind_bullet_proof(long context, byte[] commitment, byte[] nounce, byte[] proof);

    private native byte[] secp256k1_ecdsa_sign(long context, byte[] msgdata, byte[] seckey);

    private native boolean secp256k1_ecdsa_verify(long context, byte[] msgdata, byte[] sigData, byte[] pubkey);

    private native byte[] blake_2b(byte[] inputData, byte[] key);
}

