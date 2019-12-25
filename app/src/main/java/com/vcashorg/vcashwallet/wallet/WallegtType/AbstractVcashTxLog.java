package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.api.bean.ServerTxStatus;

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

    abstract public boolean isCanBeCanneled();

    abstract public void cancelTxlog();
}
