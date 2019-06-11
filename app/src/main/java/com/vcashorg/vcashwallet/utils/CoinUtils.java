/*
 * Created by wulin on 18-4-19 上午9:43.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 * Last modified 18-4-19 上午9:43.
 */

package com.vcashorg.vcashwallet.utils;

import java.text.DecimalFormat;

public class CoinUtils {

    public static String format(double count) {
        DecimalFormat df = new DecimalFormat("0.000000000");
        return df.format(count);
    }

}
