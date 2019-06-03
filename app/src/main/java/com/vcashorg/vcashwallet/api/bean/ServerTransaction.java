package com.vcashorg.vcashwallet.api.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;

import java.io.Serializable;

public class ServerTransaction implements Serializable {

    public String tx_id;
    public String sender_id;
    public String receiver_id;
    public String slate;
    public ServerTxStatus status;

    @Expose(serialize = false, deserialize = false)
    public VcashSlate slateObj;
    @Expose(serialize = false, deserialize = false)
    public boolean isSend;

    public ServerTransaction(VcashSlate sla){
        tx_id = sla.uuid;
        Gson gson = new Gson();
        slate = gson.toJson(sla);
    }
}
