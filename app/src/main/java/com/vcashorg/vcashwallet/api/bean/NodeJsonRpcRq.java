package com.vcashorg.vcashwallet.api.bean;

import com.google.gson.JsonElement;

import java.util.ArrayList;

public class NodeJsonRpcRq {
    private String jsonrpc = "2.0";
    public String method;
    public int id = 1;
    private ArrayList<JsonElement> params;

    public NodeJsonRpcRq(String method, ArrayList<JsonElement> param) {
        this.method = method;
        this.params = param;
    }
}
