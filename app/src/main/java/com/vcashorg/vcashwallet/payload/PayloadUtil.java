/*
 * created by wulin on 18-8-15 下午5:04.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 */

package com.vcashorg.vcashwallet.payload;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.vcashorg.vcashwallet.access.AccessFactory;
import com.vcashorg.vcashwallet.utils.AESUtil;
import com.vcashorg.vcashwallet.utils.CharSequenceX;
import com.vcashorg.vcashwallet.utils.DecryptionException;
import com.vcashorg.vcashwallet.wallet.VcashWallet;

import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private PayloadUtil()	{ ; }

    public static PayloadUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            instance = new PayloadUtil();
        }

        return instance;
    }

    public File getBackupFile()  {
        String directory = Environment.DIRECTORY_DOCUMENTS;
        File dir = null;
        if(context.getPackageName().contains("staging"))    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir + "/staging");
        }
        else    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir);
        }
        File file = new File(dir, strOptionalFilename);

        return file;
    }

    public JSONObject putPayload(String data, boolean external)    {

        JSONObject obj = new JSONObject();

        try {
            obj.put("version", 1);
            obj.put("payload", data);
            obj.put("external", external);
        }
        catch(JSONException je) {
            return null;
        }

        return obj;
    }

    public boolean hasPayload(Context ctx) {

        File dir = ctx.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, strFilename);
        if(file.exists())    {
            return true;
        }

        return false;
    }


    public JSONObject getPayload(List<String> mnemonic) {
        try {
            JSONObject wallet = new JSONObject();


            wallet.put("mnenonic",mnemonic);


            JSONObject meta = new JSONObject();
//            meta.put("version_name", context.getText(R.string.version_name));
            meta.put("android_release", Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE);
            meta.put("device_manufacturer", Build.MANUFACTURER == null ? "" : Build.MANUFACTURER);
            meta.put("device_model", Build.MODEL == null ? "" : Build.MODEL);
            meta.put("device_product", Build.PRODUCT == null ? "" : Build.PRODUCT);

            meta.put("pin", AccessFactory.getInstance().getPIN());

            JSONObject obj = new JSONObject();
            obj.put("wallet", wallet);
            obj.put("meta", meta);

            return obj;
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized void saveWalletToJSON(List<String> mnemonic,CharSequenceX password) throws MnemonicException.MnemonicLengthException, IOException, JSONException, DecryptionException, UnsupportedEncodingException {
//        Log.i("PayloadUtil", get().toJSON().toString());

        // save payload
        serialize(getPayload(mnemonic), password);

    }

    public synchronized boolean walletFileExists()  {
        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File walletfile = new File(dir, strFilename);
        return walletfile.exists();
    }

    private synchronized void serialize(JSONObject jsonobj, CharSequenceX password) throws IOException, JSONException, DecryptionException, UnsupportedEncodingException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File newfile = new File(dir, strFilename);
        File tmpfile = new File(dir, strTmpFilename);
        File bakfile = new File(dir, strBackupFilename);
        newfile.setWritable(true, true);
        tmpfile.setWritable(true, true);
        bakfile.setWritable(true, true);

        // prepare tmp file.
        if(tmpfile.exists()) {
            tmpfile.delete();
//            secureDelete(tmpfile);
        }

        tmpfile.createNewFile();

        String data = null;
        String jsonstr = jsonobj.toString(4);
        if(password != null) {
            data = AESUtil.encrypt(jsonstr, password, AESUtil.DefaultPBKDF2Iterations);
        }
        else {
            data = jsonstr;
        }

        JSONObject jsonObj = putPayload(data, false);
        if(jsonObj != null)    {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpfile), "UTF-8"));
            try {
                out.write(jsonObj.toString());
            } finally {
                out.close();
            }

            copy(tmpfile, newfile);
            copy(tmpfile, bakfile);
//        secureDelete(tmpfile);
        }

        //
        // test payload
        //

    }

    private synchronized JSONObject deserialize(CharSequenceX password, boolean useBackup) throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, useBackup ? strBackupFilename : strFilename);
//        Log.i("PayloadUtil", "wallet file exists: " + file.exists());
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        String str = null;

        while((str = in.readLine()) != null) {
            sb.append(str);
        }

        in.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(sb.toString());
        }
        catch(JSONException je)   {
            ;
        }
        String payload = null;
        if(jsonObj != null && jsonObj.has("payload"))    {
            payload = jsonObj.getString("payload");
        }

        // not a json stream, assume v0
        if(payload == null)    {
            payload = sb.toString();
        }

        JSONObject node = null;
        if(password == null) {
            node = new JSONObject(payload);
        }
        else {
            String decrypted = null;
            try {
                decrypted = AESUtil.decrypt(payload, password, AESUtil.DefaultPBKDF2Iterations);
            }
            catch(Exception e) {
                return null;
            }
            if(decrypted == null) {
                return null;
            }
            node = new JSONObject(decrypted);
        }

        return node;
    }

    private synchronized void secureDelete(File file) throws IOException {
        if (file.exists()) {
            long length = file.length();
            SecureRandom random = new SecureRandom();
            RandomAccessFile raf = new RandomAccessFile(file, "rws");
            for(int i = 0; i < 5; i++) {
                raf.seek(0);
                raf.getFilePointer();
                byte[] data = new byte[64];
                int pos = 0;
                while (pos < length) {
                    random.nextBytes(data);
                    raf.write(data);
                    pos += data.length;
                }
            }
            raf.close();
            file.delete();
        }
    }

    public synchronized void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private boolean isExternalStorageWritable() {

        String state = Environment.getExternalStorageState();

        if(Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }

    private synchronized void serialize(String data) throws IOException {

        String directory = Environment.DIRECTORY_DOCUMENTS;
        File dir = null;
        if(context.getPackageName().contains("staging"))    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir + "/staging");
        }
        else    {
            dir = Environment.getExternalStoragePublicDirectory(directory + strOptionalBackupDir);
        }
        if(!dir.exists())   {
            dir.mkdirs();
            dir.setWritable(true, true);
            dir.setReadable(true, true);
        }
        File newfile = new File(dir, strOptionalFilename);
        newfile.setWritable(true, true);
        newfile.setReadable(true, true);

        JSONObject jsonObj = putPayload(data, false);
        if(jsonObj != null)    {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newfile), "UTF-8"));
            try {
                out.write(jsonObj.toString());
            } finally {
                out.close();
            }
        }

        //
        // test payload
        //

    }

    public String getDecryptedBackupPayload(String data, CharSequenceX password)  {

        String encrypted = null;

        try {
            JSONObject jsonObj = new JSONObject(data);
            if(jsonObj != null && jsonObj.has("payload"))    {
                encrypted = jsonObj.getString("payload");
            }
            else    {
                encrypted = data;
            }
        }
        catch(JSONException je) {
            encrypted = data;
        }

        String decrypted = null;
        try {
            decrypted = AESUtil.decrypt(encrypted, password, AESUtil.DefaultPBKDF2Iterations);
        }
        catch (Exception e) {
//            Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
        }
        finally {
            if (decrypted == null || decrypted.length() < 1) {
//                Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
//                AppUtil.getInstance(context).restartApp();
            }
        }

        return decrypted;
    }

}
