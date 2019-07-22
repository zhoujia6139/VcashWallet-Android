package com.vcashorg.vcashwallet.utils;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vcashorg.vcashwallet.bean.Address;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AddressFileUtil {

    public static final String FILE_NAME = "Address";

    public static boolean init(Context context){
        String path = context.getExternalFilesDir(null).getAbsolutePath();
        File file = new File(path, FILE_NAME);
        try {
            if (!file.exists() && !file.createNewFile()){
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static boolean saveAddressList(Context context, List<Address> list) {
        Writer writer = null;
        try {
            String path = context.getExternalFilesDir(null).getAbsolutePath();
            File file = new File(path, FILE_NAME);
            if (!file.exists() && !file.createNewFile())
                return false;
            writer = new FileWriter(file);
            new Gson().toJson(list, writer);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static List<Address> readAddressList(Context context) {
        Reader reader = null;
        try {
            Type type = new TypeToken<List<Address>>() {}.getType();
            String path = context.getExternalFilesDir(null).getAbsolutePath();
            File file = new File(path, FILE_NAME);
            if (!file.exists())
                return null;
            reader = new FileReader(file);
            List<Address> addressList = new Gson().fromJson(reader, type);
            if(addressList == null){
                addressList = new ArrayList<>();
            }
            return addressList;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static int addAddress(Context context, Address address) {
        List<Address> addressList = readAddressList(context);
        if (addressList == null) return 0;
        for (Address l : addressList) {
            if (l.userId.equals(address.userId)) {
                return 2;
            }
        }
        addressList.add(address);
        if (saveAddressList(context, addressList)) {
            return 1;
        }
        return 0;
    }

    public static String findRemarkByAddress(Context context,String address){
        if(UIUtils.isEmpty(address)) return null;
        List<Address> addressList = readAddressList(context);
        if(addressList == null) return null;
        for (Address ad : addressList){
            if(ad.userId.equals(address)){
                return ad.remark;
            }
        }
        return null;
    }


    public static boolean deleteAddress(Context context,String id){
        List<Address> addressList = readAddressList(context);
        if (addressList == null || id == null) return false;
        for (Address ad : addressList){
            if(ad.userId.equals(id)){
                addressList.remove(ad);
                return saveAddressList(context,addressList);
            }
        }
        return false;
    }

    public static boolean updateAddress(Context context ,String id,String remark){
        List<Address> addressList = readAddressList(context);
        if (addressList == null || id == null || remark == null) return false;
        for (Address ad : addressList){
            if(ad.userId.equals(id)){
                ad.remark = remark;
                return saveAddressList(context,addressList);
            }
        }
        return false;
    }

}
