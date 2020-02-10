package com.vcashorg.vcashwallet.utils;

import android.text.TextUtils;

import com.vcashorg.vcashwallet.wallet.WalletApi;

public class VCashUtil {

    public static boolean isVCash(String tokenType){
        return "VCash".equals(tokenType);
    }

    public static boolean isVCashToken(String tokenType){
        return !TextUtils.isEmpty(tokenType) && !isVCash(tokenType);
    }

    public static String VCashUnit(String tokenType){
        if(isVCash(tokenType)){
            return "VCash";
        }else {
            return WalletApi.getTokenInfo(tokenType).Name;
        }
    }

    public static long VCashSpendable(String tokenType){
        if(isVCash(tokenType)){
            return WalletApi.getWalletBalanceInfo().spendable;
        }else {
            return WalletApi.getWalletTokenBalanceInfo(tokenType).spendable;
        }
    }
}
