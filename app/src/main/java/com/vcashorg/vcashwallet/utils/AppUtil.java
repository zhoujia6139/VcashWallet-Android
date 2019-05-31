package com.vcashorg.vcashwallet.utils;

import android.content.Context;

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

    public static byte[] getDataFromArray(ArrayList<Integer> array) {
        if (array == null || !(array instanceof ArrayList) ){
            return null;
        }
        ByteBuffer buf = ByteBuffer.allocate(1024);
        for (Integer item : array){
            byte bit = (byte) item.intValue();
            buf.put(bit);
        }
        return buf.array();
    }

    public static ArrayList<Integer> getArrFromData(byte[] data){
        if (data == null){
            return null;
        }
        ArrayList<Integer> ret = new ArrayList<Integer>();
        for (int i=0; i<data.length; i++){
            byte item = data[i];
            ret.add(new Integer(item));
        }

        return ret;
    }
}
