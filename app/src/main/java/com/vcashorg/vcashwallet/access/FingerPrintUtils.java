/*
 * created by wulin on 18-8-15 下午3:16.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 */

package com.vcashorg.vcashwallet.access;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.vcashorg.vcashwallet.utils.SPUtil;

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.util.encoders.Hex;


public class FingerPrintUtils {

    public static String getDeviceFingerPrint(Context context) {
        String fingerprint = "";
        if (TextUtils.isEmpty(SPUtil.getInstance(context).getValue(SPUtil.FP, ""))) {
            fingerprint = SPUtil.getInstance(context).getValue(SPUtil.FP, "");
        } else {
            fingerprint = Build.MANUFACTURER + Build.BRAND + Build.MODEL + Build.DEVICE;
            SPUtil.getInstance(context).setValue(SPUtil.FP, fingerprint);
        }
        return RIPEMD160(fingerprint);
    }

    public static String RIPEMD160(String data) {
        try {
            byte[] hash = data.getBytes("UTF-8");
            RIPEMD160Digest digest = new RIPEMD160Digest();
            digest.update(hash, 0, hash.length);
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            if (out != null) {
                return new String(Hex.encode(out));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";

    }

}
