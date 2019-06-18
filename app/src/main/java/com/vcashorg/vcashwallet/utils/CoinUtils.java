package com.vcashorg.vcashwallet.utils;

import java.text.DecimalFormat;

public class CoinUtils {

    public static String format(double count) {
        DecimalFormat df = new DecimalFormat("0.000000000");
        return df.format(count);
    }

}
