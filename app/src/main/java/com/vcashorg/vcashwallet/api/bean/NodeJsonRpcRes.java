package com.vcashorg.vcashwallet.api.bean;

import java.util.Map;

public class NodeJsonRpcRes {
    public String jsonrpc;
    public int id = 1;
    public Map result;
    public Map error;
}
