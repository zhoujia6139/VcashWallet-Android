package com.vcashorg.vcashwallet.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vcashorg.vcashwallet.R;
import com.vcashorg.vcashwallet.VcashApp;

import static android.content.Context.CLIPBOARD_SERVICE;


public class UIUtils {

    public static Toast mToast;

    public static void showToast(String msg) {
        showToast(msg, Toast.LENGTH_SHORT);
    }

    public static void showToast(String msg, int duration) {
        if (mToast == null) {
            mToast = Toast.makeText(getContext(), "", duration);
        }
        View view = LayoutInflater.from(getContext()).inflate(R.layout.custom_toast,null);
        TextView tv = view.findViewById(R.id.toast_content);
        tv.setText(msg);
        mToast.setView(view);
        mToast.show();
    }

    public static void showToastCenter(String msg){
        if (mToast == null) {
            mToast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
        }
        View view = LayoutInflater.from(getContext()).inflate(R.layout.custom_toast,null);
        TextView tv = view.findViewById(R.id.toast_content);
        tv.setText(msg);
        mToast.setView(view);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public static void showToastCenter(int stringId){
        if (mToast == null) {
            mToast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
        }
        View view = LayoutInflater.from(getContext()).inflate(R.layout.custom_toast,null);
        TextView tv = view.findViewById(R.id.toast_content);
        tv.setText(getString(stringId));
        mToast.setView(view);
        mToast.setGravity(Gravity.CENTER,0,0);
        mToast.show();
    }

//    /**
//     * 用于在线程中执行弹土司操作
//     */
//    public static void showToastSafely(final String msg) {
//        UIUtils.getMainThreadHandler().post(new Runnable() {
//
//            @Override
//            public void run() {
//                if (mToast == null) {
//                    mToast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
//                }
//                mToast.setText(msg);
//                mToast.show();
//            }
//        });
//    }


    /**
     * 得到上下文
     *
     * @return
     */
    public static Context getContext() {
        return VcashApp.getContext();
    }

    /**
     * 得到resources对象
     *
     * @return
     */
    public static Resources getResource() {
        return getContext().getResources();
    }

    /**
     * 得到string.xml中的字符串
     *
     * @param resId
     * @return
     */
    public static String getString(int resId) {
        return getResource().getString(resId);
    }

    /**
     * 得到string.xml中的字符串，带点位符
     *
     * @return
     */
    public static String getString(int id, Object... formatArgs) {
        return getResource().getString(id, formatArgs);
    }

    /**
     * 得到string.xml中和字符串数组
     *
     * @param resId
     * @return
     */
    public static String[] getStringArr(int resId) {
        return getResource().getStringArray(resId);
    }

    /**
     * 得到colors.xml中的颜色
     *
     * @param colorId
     * @return
     */
    public static int getColor(int colorId) {
        return getResource().getColor(colorId);
    }

    /**
     * 得到应用程序的包名
     *
     * @return
     */
    public static String getPackageName() {
        return getContext().getPackageName();
    }

//    /**
//     * 得到主线程Handler
//     *
//     * @return
//     */
//    public static Handler getMainThreadHandler() {
//        return MyApp.getMainHandler();
//    }
//
//    /**
//     * 得到主线程id
//     *
//     * @return
//     */
//    public static long getMainThreadId() {
//        return MyApp.getMainThreadId();
//    }

//    /**
//     * 安全的执行一个任务
//     *
//     * @param task
//     */
//    public static void postTaskSafely(Runnable task) {
//        int curThreadId = android.os.Process.myTid();
//        // 如果当前线程是主线程
//        if (curThreadId == getMainThreadId()) {
//            task.run();
//        } else {
//            // 如果当前线程不是主线程
//            getMainThreadHandler().post(task);
//        }
//    }

//    /**
//     * 延迟执行任务
//     *
//     * @param task
//     * @param delayMillis
//     */
//    public static void postTaskDelay(Runnable task, int delayMillis) {
//        getMainThreadHandler().postDelayed(task, delayMillis);
//    }
//
//    /**
//     * 移除任务
//     */
//    public static void removeTask(Runnable task) {
//        getMainThreadHandler().removeCallbacks(task);
//    }

    /**
     * dip-->px
     */
    public static int dip2Px(int dip) {
        // px/dip = density;
        // density = dpi/160
        // 320*480 density = 1 1px = 1dp
        // 1280*720 density = 2 2px = 1dp

        float density = getResource().getDisplayMetrics().density;
        return (int) (dip * density + 0.5f);
    }

    /**
     * px-->dip
     */
    public static int px2dip(int px) {

        float density = getResource().getDisplayMetrics().density;
        return (int) (px / density + 0.5f);
    }

    /**
     * sp-->px
     */
    public static int sp2px(int sp) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResource().getDisplayMetrics()) + 0.5f);
    }

    public static boolean isEmpty(String value){
        return TextUtils.isEmpty(value) || value.equals("null");
    }

    /**
     * getVersionName
     */
    public static String getVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    public static void copyText(Context context, String text){
        ClipboardManager cm = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("", text));
        showToastCenter(context.getString(R.string.copy_ok));
    }

    public static String getClipboardText(Context context){
        ClipboardManager cm = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData data = cm.getPrimaryClip();
        ClipData.Item item = data.getItemAt(0);
        return item.getText().toString();
    }
}