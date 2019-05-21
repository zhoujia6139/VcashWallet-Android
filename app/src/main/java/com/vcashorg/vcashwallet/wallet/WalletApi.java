package com.vcashorg.vcashwallet.wallet;

import android.content.Context;
import android.util.Log;

import com.vcashorg.vcashwallet.utils.AppUtil;

import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
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
}
