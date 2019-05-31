package com.vcashorg.vcashwallet.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    /**
     * 时间戳转年月日
     * @param sec 秒时间戳
     * @return
     */
    public static String formatDateTimeStamp(long sec) {
        Date date = new Date(sec * 1000);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return format.format(date);
    }
}
