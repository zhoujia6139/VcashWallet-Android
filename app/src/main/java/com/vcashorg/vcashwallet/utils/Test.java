package com.vcashorg.vcashwallet.utils;

public class Test {

    public static void main(String[] args) {
        String s = "arm jnida  tim     opq";
        String s2 = s.replaceAll("\\s{1,}", " ");

        String[] s1 = s.split("-");
        System.out.println(s2);
    }
}
