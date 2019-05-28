/*
 * created by wulin on 18-8-15 下午4:45.
 * Copyright (c) 2018 Blockin. All Rights Reserved.
 */

package com.vcashorg.vcashwallet.utils;

public class DecryptionException extends Exception {
    //Parameterless Constructor
    public DecryptionException() {
    }

    //Constructor that accepts a message
    public DecryptionException(String message) {
        super(message);
    }
}