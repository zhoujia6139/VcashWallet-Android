package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;
import com.vcashorg.vcashwallet.utils.UIUtils;
import com.vcashorg.vcashwallet.wallet.VcashWallet;

import java.io.Serializable;
import java.util.ArrayList;

public class VcashTokenTxLog extends AbstractVcashTxLog implements Serializable {
    public String token_type;
    public long amount_credited;
    public long amount_debited;
    public long token_amount_credited;
    public long token_amount_debited;
    public final ArrayList<String> inputs = new ArrayList<>();
    public final ArrayList<String> outputs = new ArrayList<>();
    public final ArrayList<String> token_inputs = new ArrayList<>();
    public final ArrayList<String> token_outputs = new ArrayList<>();

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

    public void cancelTxlog(){
        ArrayList<VcashOutput> walletOutputs = VcashWallet.getInstance().outputs;
        ArrayList<VcashTokenOutput> walletTokenOutputs = VcashWallet.getInstance().token_outputs_dic.get(this.token_type);

        if (tx_type == TxLogEntryType.TxSent){
            for (String commitment: inputs){
                for (VcashOutput item:walletOutputs){
                    if (commitment.equals(item.commitment)){
                        item.status = VcashOutput.OutputStatus.Unspent;
                    }
                }
            }

            for (String commitment: token_inputs){
                for (VcashTokenOutput item:walletTokenOutputs){
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

        for (String commitment: token_outputs){
            for (VcashTokenOutput item:walletTokenOutputs){
                if (commitment.equals(item.commitment)){
                    item.status = VcashOutput.OutputStatus.Spent;
                }
            }
        }

        VcashWallet.getInstance().syncOutputInfo();
        VcashWallet.getInstance().syncTokenOutputInfo();

        tx_type = ((tx_type == TxLogEntryType.TxSent)?TxLogEntryType.TxSentCancelled:TxLogEntryType.TxReceivedCancelled);
        server_status = ServerTxStatus.TxCanceled;
    }

}
