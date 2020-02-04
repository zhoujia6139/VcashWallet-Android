package com.vcashorg.vcashwallet.utils;

import com.vcashorg.vcashwallet.wallet.WalletApi;

public class VCashUtil {

    public static boolean isVCash(String tokenType){
        return "VCash".equals(tokenType);
    }

    public static long VCashSpendable(String tokenType){
        if(isVCash(tokenType)){
            return WalletApi.getWalletBalanceInfo().spendable;
        }else {
            return WalletApi.getWalletTokenBalanceInfo(tokenType).spendable;
        }
    }
}
