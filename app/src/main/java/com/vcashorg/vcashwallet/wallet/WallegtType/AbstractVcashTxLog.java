package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.utils.UIUtils;

import java.io.Serializable;

public abstract class AbstractVcashTxLog extends Object implements Serializable {
    public short tx_id;
    public String tx_slate_id;
    public String parter_id;
    public long create_time;
    public long confirm_time;
    public long confirm_height;
    public VcashTxLog.TxLogConfirmType confirm_state;
    public ServerTxStatus server_status;
    public TxLogEntryType tx_type;
    public long fee;
    public String signed_slate_msg;

    public boolean isCanBeCanneled(){
        if (tx_type == TxLogEntryType.TxSent && confirm_state == VcashTxLog.TxLogConfirmType.DefaultState){
            return true;
        }else if(tx_type == TxLogEntryType.TxReceived && UIUtils.isEmpty(parter_id) && confirm_state == VcashTxLog.TxLogConfirmType.DefaultState){
            return true;
        }

        return false;
    }

    public boolean isCanBeAutoCanneled (){
        if ((tx_type == TxLogEntryType.TxSent || tx_type == TxLogEntryType.TxReceived) &&
        confirm_state == VcashTxLog.TxLogConfirmType.DefaultState) {
            return true;
        }

        return false;
    }

    abstract public void cancelTxlog();

    public enum TxLogEntryType{
        ConfirmedCoinbaseOrTokenIssue(0),
        TxReceived(1),
        TxSent(2),
        TxReceivedCancelled(3),
        TxSentCancelled(4);

        private final int code;

        TxLogEntryType(int code) {
            this.code = code;
        }

        public static TxLogEntryType locateEnum(int code) {
            for (TxLogEntryType type: TxLogEntryType.values()){
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
}
