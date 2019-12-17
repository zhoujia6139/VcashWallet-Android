package com.vcashorg.vcashwallet.api.bean;

public enum ServerTxStatus
{
    TxDefaultStatus(0),
    TxReceiverd(1),
    TxFinalized(2),
    TxCanceled(3),
    TxClosed(4);

    private final int code;

    ServerTxStatus(int code) {
        this.code = code;
    }

    public static ServerTxStatus locateEnum(int code) {
        for (ServerTxStatus type: ServerTxStatus.values()){
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
