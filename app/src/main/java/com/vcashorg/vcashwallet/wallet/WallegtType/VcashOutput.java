package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.wallet.VcashWallet;

public class VcashOutput {
    public String commitment;
    //VcashKeychainPath.pathData hex format
    public String keyPath;
    public long mmr_index;
    public long value;
    public long height;
    public long lock_height;
    public boolean is_coinbase;
    public OutputStatus status;
    public short tx_log_id;
    public byte[] blinding;

    public enum OutputStatus{
        Unconfirmed,
        Unspent,
        Locked,
        Spent
    }

    public boolean isSpendable(){
        if (status == OutputStatus.Unspent &&
        lock_height < VcashWallet.getInstance().getChainHeight()){
            return true;
        }
        return false;
    }
}
