package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;

public abstract class VcashTxBaseObject {

    abstract public byte[] computePayload(boolean isForHash);

    public byte[] blake2bHash(){
        byte[] payload = computePayload(true);
        return NativeSecp256k1.instance().blake2b(payload, null);
    }
}
