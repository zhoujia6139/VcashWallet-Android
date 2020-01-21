package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.VcashWallet;

import java.io.Serializable;
import java.util.ArrayList;

public class VcashTxLog extends AbstractVcashTxLog implements Serializable {
    public long amount_credited;
    public long amount_debited;
    public final ArrayList<String> inputs = new ArrayList<String>();
    public final ArrayList<String> outputs = new ArrayList<String>();

    public void appendInput(String commitment){
        inputs.add(commitment);
    }

    public void appendOutput(String commitment){
        outputs.add(commitment);
    }

    public boolean isCanBeCanneled(){
        if (tx_type == TxLogEntryType.TxSent && confirm_state == TxLogConfirmType.DefaultState){
            return true;
        }else if(tx_type == TxLogEntryType.TxReceived && UIUtils.isEmpty(parter_id) && confirm_state == TxLogConfirmType.DefaultState){
            return true;
        }

        return false;
    }

    public void cancelTxlog(){
        ArrayList<VcashOutput> walletOutputs = VcashWallet.getInstance().outputs;
        if (tx_type == TxLogEntryType.TxSent){
            for (String commitment: inputs){
                for (VcashOutput item:walletOutputs){
                    if (commitment.equals(item.commitment)){
                        item.status = VcashOutput.OutputStatus.Unspent;
                    }
                }
            }
        }

        for (String commitment: outputs){
            for (VcashOutput item:walletOutputs){
                if (commitment.equals(item.commitment)){
                    item.status = VcashOutput.OutputStatus.Spent;
                }
            }
        }
        VcashWallet.getInstance().syncOutputInfo();

        tx_type = ((tx_type == VcashTxLog.TxLogEntryType.TxSent)?VcashTxLog.TxLogEntryType.TxSentCancelled:VcashTxLog.TxLogEntryType.TxReceivedCancelled);
        server_status = ServerTxStatus.TxCanceled;
    }




    public enum TxLogConfirmType{
        DefaultState(0),
        LoalConfirmed(1),
        NetConfirmed(2);

        private final int code;

        TxLogConfirmType(int code) {
            this.code = code;
        }

        public static TxLogConfirmType locateEnum(int code) {
            for (TxLogConfirmType type: TxLogConfirmType.values()){
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
