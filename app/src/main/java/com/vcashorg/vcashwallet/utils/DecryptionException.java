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