package com.vcashorg.vcashwallet.bean;

import java.util.List;

public class Demo {

    public int res_code;
    public String err_msg;
    public List<DemoItem> demo;


    public class DemoItem{
        public String id;
        public String name;
        public String appid;
        public String showtype;
    }
}
