package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;

import java.io.Serializable;

public abstract class VcashTxBaseObject implements Serializable {

    abstract public byte[] computePayload(boolean isForHash);

    public byte[] blake2bHash(){
        byte[] payload = computePayload(true);
        return NativeSecp256k1.instance().blake2b(payload, null);
    }
}
