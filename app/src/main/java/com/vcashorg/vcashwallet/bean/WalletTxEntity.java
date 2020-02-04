package com.vcashorg.vcashwallet.bean;

import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.vcashorg.vcashwallet.api.bean.ServerTransaction;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTokenTxLog;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashTxLog;

public class WalletTxEntity implements MultiItemEntity {

    public static final int TYPE_SERVER_TX = 1;
    public static final int TYPE_TX_LOG = 2;
    public static final int TYPE_TX_ONGOING = 3;
    public static final int TYPE_TX_COMPLETE = 4;
    public static final int TYPE_TOKEN_TX_LOG = 5;

    private int itemType;

    public ServerTransaction serverTxEntity;
    public VcashTxLog txLogEntity;
    public VcashTokenTxLog tokenTxLogEntity;

    public ServerTransaction getServerTxEntity() {
        return serverTxEntity;
    }

    public void setServerTxEntity(ServerTransaction serverTxEntity) {
        this.serverTxEntity = serverTxEntity;
    }

    public VcashTxLog getTxLogEntity() {
        return txLogEntity;
    }

    public void setTxLogEntity(VcashTxLog txLogEntity) {
        this.txLogEntity = txLogEntity;
    }

    public VcashTokenTxLog getTokenTxLogEntity(){
        return tokenTxLogEntity;
    }

    public void setTokenTxLogEntity(VcashTokenTxLog tokenTxLogEntity){
        this.tokenTxLogEntity = tokenTxLogEntity;
    }

    @Override
    public int getItemType() {
        return this.itemType;
    }


    public void setItemType(int itemType) {
        this.itemType = itemType;
    }
}
