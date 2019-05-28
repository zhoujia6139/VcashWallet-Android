/*
 * created by wulin on 18-8-15 下午3:16.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 */

package com.vcashorg.vcashwallet.access;

import android.content.Context;

import com.vcashorg.vcashwallet.utils.CharSequenceX;
import com.vcashorg.vcashwallet.utils.SPUtil;

import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.util.UUID;

//import android.util.Log;

public class AccessFactory {

    public static final int MIN_PIN_LENGTH = 5;
    public static final int MAX_PIN_LENGTH = 8;

    private static String _pin = "";

    private static boolean isLoggedIn = false;

    private static Context context = null;
    private static AccessFactory instance = null;

    private AccessFactory() {
    }

    public static AccessFactory getInstance(Context ctx) {
        context = ctx;
        if (instance == null) {
            instance = new AccessFactory();
        }
        return instance;
    }

    public static AccessFactory getInstance() {

        if (instance == null) {
            instance = new AccessFactory();
        }

        return instance;
    }


    public String getPIN() {
        return _pin;
    }

    public void setPIN(String pin) {
        _pin = pin;
    }


    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean logged) {
        isLoggedIn = logged;
    }

    public String getHash(String randomKey, CharSequenceX fixedKey, int iterations) {

        byte[] data = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            {
                // n rounds of SHA256
                data = md.digest((randomKey + fixedKey.toString()).getBytes("UTF-8"));
                // first hash already done above
                for (int i = 1; i < iterations; i++) {
                    data = md.digest(data);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (data != null) {
            return new String(Hex.encode(data));
        } else {
            return null;
        }

    }

    public boolean validateHash(String hash, String randomKey, CharSequenceX fixedKey, int iterations) {
        String _hash = null;
        _hash = getHash(randomKey, fixedKey, iterations);
        return hash.equals(_hash);

    }

    public String getGUID() {
        return SPUtil.getInstance(context).getValue(SPUtil.GUID_PRE, "") + FingerPrintUtils.getDeviceFingerPrint(context);
    }

    public String createGUID() {
        String guid = UUID.randomUUID().toString();
        SPUtil.getInstance(context).setValue(SPUtil.GUID_PRE, guid);
        return guid + FingerPrintUtils.getDeviceFingerPrint(context);
    }

}
