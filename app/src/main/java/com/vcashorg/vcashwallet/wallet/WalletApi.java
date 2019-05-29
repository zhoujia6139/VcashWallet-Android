package com.vcashorg.vcashwallet.wallet;

import android.content.Context;
import android.util.Log;

import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WalletApi {
    private static  Context context;

    public static void setWalletContext(Context con){
        context = con;
    }

    public static List<String> getAllPhraseWords(){
        return MnemonicHelper.instance(context).getWordList();
    }

    public static List<String> generateMnemonicPassphrase(){
        List<String> strList = null;
        try {
            byte[] seed = MnemonicHelper.instance(context).randomBytes(32);
            strList = MnemonicHelper.instance(context).mnemoicFromBytes(seed);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }

        return strList;
    }

    public static byte[] getSeed(){
        return  MnemonicHelper.instance(context).randomBytes(32);
    }

    public static List<String> generateMnemonicBySeed(byte[] seed){
        try {
            return  MnemonicHelper.instance(context).mnemoicFromBytes(seed);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean createWallet(List<String> wordsArr, String password){
        if (wordsArr == null){
            wordsArr = MnemonicHelper.split("layer floor valley flag dawn dress sponsor whale illegal session juice beef scout mammal snake cage river lemon easily away title else layer limit");
        }
        byte[] entropy = MnemonicHelper.instance(context).toEntropy(wordsArr);
        if (entropy != null){
            DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(entropy);
            byte[] secret = masterKey.getPrivKeyBytes();
            String temp = AppUtil.hex(secret);
            Log.d("WalletApi createWallet", temp);
            VcashKeychain keyChain = new VcashKeychain(masterKey);
            VcashWallet.createVcashWallet(keyChain);
            return true;
        }

        return false;
    }

    public static void clearWallet(){
        return;
    }

    public static String getWalletUserId(){
        if (VcashWallet.getInstance() != null){
            return VcashWallet.getInstance().mUserId;
        }

        return null;
    }

    public static WalletBalanceInfo getWalletBalanceInfo(){
        long total = 0;
        long locked = 0;
        long unconfirmed = 0;
        long spendable = 0;

        for (VcashOutput output :VcashWallet.getInstance().outputs){
            switch (output.status){
                case Unconfirmed:{
                    total += output.value;
                    unconfirmed += output.value;
                    break;
                }
                case Unspent:{
                    total += output.value;
                    if (output.isSpendable()){
                        spendable += output.value;
                    }
                    break;
                }

                case Locked:{
                    locked += output.value;
                    break;
                }

                default:
                    break;
            }
        }
        WalletBalanceInfo info = new WalletApi.WalletBalanceInfo();
        info.total = total;
        info.spendable = spendable;
        info.locked = locked;
        info.unconfirmed = unconfirmed;
        return info;
    }

    public static void checkWalletUtxo(final WalletCallback callback){
        ArrayList<VcashOutput> arr = new ArrayList<>();
        NodeApi.getOutputsByPmmrIndex(0, arr, new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data){
                if (yesOrNo){
                    if (callback != null){
                        callback.onCall(true, null);
                    }
                }
                else {
                    if (callback != null){
                        callback.onCall(false, null);
                    }
                }
            }
        });
    }

    public static class WalletBalanceInfo {
        public long total;
        public long locked;
        public long unconfirmed;
        public long spendable;
    }
}
