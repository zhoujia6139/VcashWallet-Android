package com.vcashorg.vcashwallet.bean;

public class MnemonicData {

    public static final int STATE_UNCHECK = 0;
    public static final int STATE_CHECK_TRUE = 1;
    public static final int STATE_CHECK_FALSE = -1;
    public static final int STATE_CHECK_NEXT = 2;

    public int state;

    public int num;

    public String data;
}
