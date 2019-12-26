package com.vcashorg.vcashwallet.api.bean;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

public class JsonRpcRes {
    public String jsonrpc;
    public int id = 1;
    public VcashSlate resSlate;
    public Map error;

    public static class JsonRpcResTypeAdapter extends TypeAdapter<JsonRpcRes> {
        @Override
        public void write(JsonWriter jsonWriter, JsonRpcRes res) throws IOException {
            jsonWriter.beginObject();

            jsonWriter.endObject();
        }

        @Override
        public JsonRpcRes read(JsonReader jsonReader) throws IOException {
            JsonRpcRes res = new JsonRpcRes();
            jsonReader.beginObject();
            while (jsonReader.hasNext()){
                switch (jsonReader.nextName()){
                    case "jsonrpc": {
                        res.jsonrpc = jsonReader.nextString();
                    }
                    break;
                    case "id": {
                        res.id = jsonReader.nextInt();
                    }
                    break;
                    case "result": {
                        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
                        Type type = new TypeToken<Map<String, JsonElement>>() {}.getType();
                        Map<String, JsonElement> slateStrMap = gson.fromJson(jsonReader, type);
                        JsonElement slateElement = slateStrMap.get("Ok");
                        Gson slate_gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, new VcashSlate.VcashSlateTypeAdapter()).serializeNulls().create();
                        res.resSlate =  slate_gson.fromJson(slateElement, VcashSlate.class);
                    }
                    break;
                    case "error": {
                        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
                        Type type = new TypeToken<Map<String, String>>() {}.getType();
                        Map<String, String> errorMap = gson.fromJson(jsonReader, type);
                        res.error = errorMap;
                    }
                    break;
                }
            }
            jsonReader.endObject();
            return res;
        }
    }
}
