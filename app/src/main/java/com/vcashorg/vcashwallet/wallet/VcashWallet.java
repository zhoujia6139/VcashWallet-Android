package com.vcashorg.vcashwallet.wallet;

import com.vcashorg.vcashwallet.utils.AppUtil;

public class VcashWallet {
    private static VcashWallet instance = null;
    private VcashKeychain mKeyChain;
    public String mUserId = null;

    private VcashWallet(VcashKeychain keychain){
        mKeyChain = keychain;
        mUserId = this.createUserId();
    }

    public static void createVcashWallet(VcashKeychain keychain){
        if (instance == null){
            instance = new VcashWallet(keychain);
        }
    }

    public static VcashWallet getInstance(){
        return instance;
    }

    private String createUserId(){
        byte[] key = this.mKeyChain.dervieKey(0, new VcashKeychainPath(4, 0, 0, 0,0));
        return AppUtil.hex(key);
    }
}
