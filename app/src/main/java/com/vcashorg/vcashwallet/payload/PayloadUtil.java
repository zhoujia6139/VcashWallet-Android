/*
 * created by wulin on 18-8-15 下午5:04.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 */

package com.vcashorg.vcashwallet.payload;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import com.vcashorg.vcashwallet.utils.UIUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.SecureRandom;
import java.util.List;

//import android.util.Log;

public class PayloadUtil {

    private final static String dataDir = "wallet";
    private final static String strFilename = "vcashwallet.dat";
    private final static String strTmpFilename = "vcashwallet.tmp";
    private final static String strBackupFilename = "vcashwallet.sav";

    private final static String strOptionalBackupDir = "/vcashwallet";
    private final static String strOptionalFilename = "vcashwallet.txt";

    private static Context context = null;

    private static PayloadUtil instance = null;

    private PayloadUtil() {

    }

    public static PayloadUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new PayloadUtil();
        }

        return instance;
    }


    public boolean saveMnemonicToSDCard(String content) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            UIUtils.showToast("No SDCard");
            return false;
        }

        String filePath = Environment.getExternalStorageDirectory().getPath() + "/vcashwallet/";

        File dictionaryFile = new File(filePath);
        if(!dictionaryFile.exists()){
            dictionaryFile.mkdirs();
        }

        File sdFile = new File(filePath + strFilename);

        try {
            if(!sdFile.exists()){
                sdFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(sdFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(content);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        UIUtils.showToast("SAVE SUCCESS");
        return true;

    }

    public String readMnemonicFromSDCard() {
        String content = null;

        File sdFile = new File(Environment.getExternalStorageDirectory().getPath() + "/vcashwallet/" + strFilename);

        try {
            FileInputStream fis = new FileInputStream(sdFile);
            ObjectInputStream ois = new ObjectInputStream(fis);
            content = (String) ois.readObject();
            ois.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return content;
    }

    public boolean ifMnemonicFileExist() {
        try {
            File f = new File(Environment.getExternalStorageDirectory().getPath() + "/vcashwallet/" + strFilename);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
