package com.vcashorg.vcashwallet.utils;

import android.content.Context;

import org.bitcoinj.core.Utils;

import java.io.File;
import java.security.Security;

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
            com.blockin.wallet.prng.PRNGFixes.apply();
        }
        catch(Exception e0) {
            //
            // some Android 4.0 devices throw an exception when PRNGFixes is re-applied
            // removing provider before apply() is a workaround
            //
            Security.removeProvider("LinuxPRNG");
            try {
                com.blockin.wallet.prng.PRNGFixes.apply();
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
}
