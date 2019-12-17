package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.wallet.VcashWallet;

public class VcashOutput {
    public String commitment;
    public String keyPath;
    public long mmr_index;
    public long value;
    public long height;
    public long lock_height;
    public boolean is_coinbase;
    public OutputStatus status;
    public short tx_log_id;

    public enum OutputStatus{
        Unconfirmed(0),
        Unspent(1),
        Locked(2),
        Spent(3);

        private final int code;

        OutputStatus(int code) {
            this.code = code;
        }

        public static OutputStatus locateEnum(int code) {
            for (OutputStatus type: OutputStatus.values()){
                if (code == type.code()){
                    return type;
                }
            }
            return null;
        }

        public int code() {
            return code;
        }
    }

    public boolean isSpendable(){
        if (status == OutputStatus.Unspent &&
        lock_height <= VcashWallet.getInstance().getChainHeight()){
            return true;
        }
        return false;
    }
}
