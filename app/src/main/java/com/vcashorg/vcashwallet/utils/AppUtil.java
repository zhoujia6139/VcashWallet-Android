package com.vcashorg.vcashwallet.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.vcashorg.vcashwallet.prng.PRNGFixes;

import org.bitcoinj.core.Utils;

import java.io.File;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;

public class AppUtil {
    private static AppUtil instance = null;
    private static Context context = null;

    public static AppUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new AppUtil();
        }

        return instance;
    }

    public void applyPRNGFixes()    {
        try {
            PRNGFixes.apply();
        }
        catch(Exception e0) {
            //
            // some Android 4.0 devices throw an exception when PRNGFixes is re-applied
            // removing provider before apply() is a workaround
            //
            Security.removeProvider("LinuxPRNG");
            try {
                PRNGFixes.apply();
            }
            catch(Exception e1) {
//                Toast.makeText(context, R.string.cannot_launch_app, Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
        }
    }

    public static String hex(byte[] b) {
        return Utils.HEX.encode(b);
    }

    public static byte[] decode(String hex) {
        return Utils.HEX.decode(hex);
    }

    public static byte[] randomBytes(int len) {
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[len];
        random.nextBytes(seed);
        return seed;
    }

    public static byte[] zeroByteArray(int len){
        byte[] ret = new byte[len];
        for (int i=0; i<len; i++){
            ret[i] = 0;
        }
        return ret;
    }

    public static long getCurrentTimeSecs(){
        return System.currentTimeMillis()/1000;
    }

    public static byte[][] ArrayListToByteArr(ArrayList<byte[]> arr){
        if (arr == null || arr.size() == 0){
            return null;
        }
        byte[][] retArr = new byte[arr.size()][];
        int i = 0;
        for (byte[] item :arr){
            retArr[i] = item;
            i++;
        }
        return retArr;
    }

    public static byte[] intArrToByteArr(ArrayList<Integer> intArr) {
        byte[] byteArr = new byte[intArr.size()];
        int i = 0;
        for (int item :intArr){
            byte temp = (byte)(item & 0xff);
            byteArr[i] = temp;
            i++;
        }
        return byteArr;
    }

    public static String getByteStrFromByteArr(byte[] data){
        if (data == null){
            return null;
        }
        int[] intArr = new int[data.length];
        for (int i=0; i<data.length; i++){
            byte item = data[i];
            int temp = item & 0xff;
            intArr[i] = temp;
        }

        Gson gson = new Gson();
        String retStr = gson.toJson(intArr, int[].class);
        return retStr;
    }

    public static byte[] BufferToByteArr(ByteBuffer byteBuffer){
        int len = byteBuffer.limit() - byteBuffer.position();
        byte[] bytes = new byte[len];

        if(byteBuffer.isReadOnly()){
            return null;
        }else {
            byteBuffer.get(bytes);
        }
        return bytes;
    }
}
