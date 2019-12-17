package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.VcashWallet;

import java.io.Serializable;
import java.util.ArrayList;

public class VcashTokenTxLog implements Serializable {
    public short tx_id;
    public String tx_slate_id;
    public String parter_id;
    public VcashTokenTxLog.TokenTxLogEntryType tx_type;
    public long create_time;
    public long confirm_time;
    public long confirm_height;
    public VcashTxLog.TxLogConfirmType confirm_state;
    public ServerTxStatus server_status;
    public String token_type;
    public long amount_credited;
    public long amount_debited;
    public long token_amount_credited;
    public long token_amount_debited;
    public long fee;
    public final ArrayList<String> inputs = new ArrayList<>();
    public final ArrayList<String> outputs = new ArrayList<>();
    public final ArrayList<String> token_inputs = new ArrayList<>();
    public final ArrayList<String> token_outputs = new ArrayList<>();
    public String signed_slate_msg;

    public void appendInput(String commitment){
        inputs.add(commitment);
    }

    public void appendOutput(String commitment){
        outputs.add(commitment);
    }

    public void appendTokenInput(String commitment){
        token_inputs.add(commitment);
    }

    public void appendTokenOutput(String commitment){
        token_outputs.add(commitment);
    }

    public boolean isCanBeCanneled(){
        if (tx_type == TokenTxLogEntryType.TokenTxSent && confirm_state == VcashTxLog.TxLogConfirmType.DefaultState){
            return true;
        }else if(tx_type == TokenTxLogEntryType.TokenTxReceived && UIUtils.isEmpty(parter_id) && confirm_state == VcashTxLog.TxLogConfirmType.DefaultState){
            return true;
        }

        return false;
    }

    public void cancelTokenTxlog(){
        ArrayList<VcashOutput> walletOutputs = VcashWallet.getInstance().outputs;
        if (tx_type == TokenTxLogEntryType.TokenTxSent){
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

        tx_type = ((tx_type == TokenTxLogEntryType.TokenTxSent)?TokenTxLogEntryType.TokenTxSentCancelled:TokenTxLogEntryType.TokenTxReceivedCancelled);
        server_status = ServerTxStatus.TxCanceled;
    }


    public enum TokenTxLogEntryType{
        /// Sent transaction that was rolled back by user
        TokenIssue(0),
        /// Outputs created when a transaction is received
        TokenTxReceived(1),
        /// Inputs locked + change outputs when a transaction is created
        TokenTxSent(2),
        /// Received token transaction that was rolled back by user
        TokenTxReceivedCancelled(3),
        /// Sent token transaction that was rolled back by user
        TokenTxSentCancelled(4);

        private final int code;

        TokenTxLogEntryType(int code) {
            this.code = code;
        }

        public static TokenTxLogEntryType locateEnum(int code) {
            for (TokenTxLogEntryType type: TokenTxLogEntryType.values()){
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
