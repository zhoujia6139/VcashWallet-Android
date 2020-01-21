package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.wallet.VcashWallet;

public class VcashTokenOutput {
    public String commitment;
    public String token_type;
    public String keyPath;
    public long mmr_index;
    public long value;
    public long height;
    public long lock_height;
    public boolean is_token_issue;
    public VcashOutput.OutputStatus status;
    public short tx_log_id;

    public boolean isSpendable(){
        if (status == VcashOutput.OutputStatus.Unspent &&
                lock_height <= VcashWallet.getInstance().getChainHeight()){
            return true;
        }
        return false;
    }
}
