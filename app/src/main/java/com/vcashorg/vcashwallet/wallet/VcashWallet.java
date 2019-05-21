package com.vcashorg.vcashwallet.wallet;

public class VcashWallet {
    private static VcashWallet instance = null;
    private VcashKeychain mKeyChain;

    private VcashWallet(VcashKeychain keychain){
        mKeyChain = keychain;
    }

    public static void createVcashWallet(VcashKeychain keychain){
        if (instance == null){
            instance = new VcashWallet(keychain);
        }
    }

    public static VcashWallet getInstance(){
        return instance;
    }
}
