package com.vcashorg.vcashwallet.api.bean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;

import java.io.IOException;
import java.util.ArrayList;

public class JsonRpcRq {
    public String jsonrpc = "2.0";
    public String method = "receive_tx";
    public int id = 1;
    public ArrayList<VcashSlate> params;

    public JsonRpcRq(VcashSlate param) {
        params = new ArrayList<>();
        params.add(param);
        params.add(null);
        params.add(null);
    }

    public class JsonRpcRqTypeAdapter extends TypeAdapter<JsonRpcRq> {
        @Override
        public void write(JsonWriter jsonWriter, JsonRpcRq req) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name("jsonrpc").value(req.jsonrpc);
            jsonWriter.name("method").value(req.method);
            jsonWriter.name("id").value(req.id);
            VcashSlate slate = new VcashSlate();
            final Gson slate_gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, slate.new VcashSlateTypeAdapter()).create();
            String paramStr = slate_gson.toJson(req.params, new TypeToken<ArrayList<VcashSlate>>(){}.getType());
            jsonWriter.name("params").jsonValue(paramStr);
            jsonWriter.endObject();
        }

        @Override
        public JsonRpcRq read(JsonReader jsonReader) throws IOException {
            JsonRpcRq req = new JsonRpcRq(null);
            return req;
        }
    }
}
