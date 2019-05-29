package com.vcashorg.vcashwallet.utils;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vcashorg.vcashwallet.payload.PayloadUtil;

import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class ValidateUtil {

    public static boolean validate(String psw){
        if(PayloadUtil.getInstance(UIUtils.getContext()).ifMnemonicFileExist()){
            String words = PayloadUtil.getInstance(UIUtils.getContext()).readMnemonicFromSDCard();
            try {
                String json = AESUtil.decrypt(words,new CharSequenceX(psw),AESUtil.DefaultPBKDF2Iterations);
                if(!TextUtils.isEmpty(json)){
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (InvalidCipherTextException e) {
                UIUtils.showToast("Incorrect Password");
                e.printStackTrace();
            } catch (DecryptionException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static List<String> validate2(String psw){
        if(PayloadUtil.getInstance(UIUtils.getContext()).ifMnemonicFileExist()){
            String words = PayloadUtil.getInstance(UIUtils.getContext()).readMnemonicFromSDCard();
            try {
                String json = AESUtil.decrypt(words,new CharSequenceX(psw),AESUtil.DefaultPBKDF2Iterations);
                return new Gson().fromJson(json, new TypeToken<List<String>>() {}.getType());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (InvalidCipherTextException e) {
                UIUtils.showToast("Incorrect Password");
                e.printStackTrace();
            } catch (DecryptionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
