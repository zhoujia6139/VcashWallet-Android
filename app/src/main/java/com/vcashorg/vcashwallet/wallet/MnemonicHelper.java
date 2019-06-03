package com.vcashorg.vcashwallet.wallet;

import android.content.Context;
import android.util.Log;


import com.vcashorg.vcashwallet.utils.AppUtil;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;


public class MnemonicHelper {
    public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";

    private static MnemonicHelper sIntance;

    private MnemonicCode mc;

    private Context context;

    private MnemonicHelper(Context context) {
        try {
            this.context = context;
            InputStream wis = context.getResources().getAssets().open("en.txt");
            if (wis != null) {
                mc = new MnemonicCode(wis, BIP39_ENGLISH_SHA256);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MnemonicHelper instance(Context context) {
        if (sIntance == null) {
            sIntance = new MnemonicHelper(context);
        }
        return sIntance;
    }

    public List<String> getWordList() {
        return mc.getWordList();
    }


    public List<String> mnemoicFromBytes(byte[] seeds) throws IOException, MnemonicException.MnemonicLengthException {
        List<String> mnemoics = null;
        mnemoics = mc.toMnemonic(seeds);
        return mnemoics;
    }

    public static String flat(List<String> mnemoics) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mnemoics.size(); i++) {
            sb.append(mnemoics.get(i));
            if (i != mnemoics.size() - 1) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    public static ArrayList<String> split(String mnemoics) {
        String[] splitArr = mnemoics.split(" ");
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < splitArr.length; i++) {
            result.add(splitArr[i]);
        }
        return result;
    }

    public byte[] toSeed(String mnemoics) {
        List<String> mList = split(mnemoics);

        try {
            return mc.toEntropy(mList);
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicWordException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicChecksumException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] toEntropy(List<String> words) {
        try {
            return mc.toEntropy(words);
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicWordException e) {
            e.printStackTrace();
        } catch (MnemonicException.MnemonicChecksumException e) {
            e.printStackTrace();
        }

        return null;
    }


    public String toMnemoicStr(String seed) {
        String mnemoicStr = null;

        List<String> mnemoics = null;
        try {
            mnemoics = mc.toMnemonic(AppUtil.decode(seed));
        } catch (MnemonicException.MnemonicLengthException e) {
            e.printStackTrace();
        }
        mnemoicStr = flat(mnemoics);

        return mnemoicStr;
    }


}
