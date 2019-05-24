package com.vcashorg.vcashwallet.wallet;

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

    private native long secp256k1_ctx_create();

    private native byte[] secp256k1_bind_switch(long context, long value, byte[] key);

    private native boolean secp256k1_verify_ec_ecretKey(long context, byte[] data);

    private native byte[] secp256k1_get_commitment(long context, long value, byte[] key);

}

