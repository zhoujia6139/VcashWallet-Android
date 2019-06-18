package com.miner.update.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class FileUtils {

	public static String getDownloadApkCachePath() {

		String appCachePath = null;


		if (checkSDCard()) {
			appCachePath = Environment.getExternalStorageDirectory() + "/ApkVersionPath/" ;
		} else {
			appCachePath = Environment.getDataDirectory().getPath() + "/ApkVersionPath/" ;
		}
		File file = new File(appCachePath);
		if (!file.exists()) {
			file.mkdirs();
		}
		return appCachePath;
	}



	/**
	 * 检查有没有sd卡
	 */
	public static boolean checkSDCard() {

		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);

	}

	public static boolean checkAPKIsExists(Context context, String downloadPath) {
		File file = new File(downloadPath);
		boolean result = false;
		if (file.exists()) {
			try {
				PackageManager pm = context.getPackageManager();
				PackageInfo info = pm.getPackageArchiveInfo(downloadPath,
						PackageManager.GET_ACTIVITIES);
				//判断安装包存在并且包名一样并且版本号不一样
				Log.e("update","本地安装包版本号：" + info.versionCode + "\n 当前app版本号：" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
				if (info != null && context.getPackageName().equalsIgnoreCase(info.packageName) && context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode != info.versionCode) {
					result = true;
				}
			} catch (Exception e) {
				result = false;
			}
		}
		return result;

	}



}