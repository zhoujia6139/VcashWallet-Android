package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.wallet.VcashWallet;

import java.io.Serializable;
import java.util.ArrayList;

public class VcashTxLog implements Serializable {
    public short tx_id;
    public String tx_slate_id;
    public String parter_id;
    public TxLogEntryType tx_type;
    public long create_time;
    public long confirm_time;
    public TxLogConfirmType confirm_state;
    public ServerTxStatus server_status;
    public long amount_credited;
    public long amount_debited;
    public long fee;
    public ArrayList<String> inputs;
    public ArrayList<String> outputs;

    public void appendInput(String commitment){
        if (inputs == null){
            inputs = new ArrayList<String>();
        }
        inputs.add(commitment);
    }

    public void appendOutput(String commitment){
        if (outputs == null){
            outputs = new ArrayList<String>();
        }
        outputs.add(commitment);
    }

    public boolean isCanBeCanneled(){
        if (tx_type == TxLogEntryType.TxSent && confirm_state == TxLogConfirmType.DefaultState){
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


    public enum TxLogEntryType{
        ConfirmedCoinbase,
        TxReceived,
        TxSent,
        TxReceivedCancelled,
        TxSentCancelled,
    }

    public enum TxLogConfirmType{
        DefaultState,
        LoalConfirmed,
        NetConfirmed,
    }

}
