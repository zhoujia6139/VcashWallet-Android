package com.vcashorg.vcashwallet.wallet;

import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;

public class VcashWallet {
    private static VcashWallet instance = null;
    private VcashKeychain mKeyChain;
    public String mUserId = null;

    private VcashWallet(VcashKeychain keychain){
        mKeyChain = keychain;
        mUserId = this.createUserId();
        SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.USER_ID,mUserId);
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
