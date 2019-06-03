package com.vcashorg.vcashwallet.api.bean;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class FinalizeTxInfo {
    public String tx_id;
    public ServerTxStatus code;

    public class FinalizeTxInfoTypeAdapter extends TypeAdapter<FinalizeTxInfo> {
        @Override
        public void write(JsonWriter jsonWriter, FinalizeTxInfo tx) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name("tx_id").value(tx.tx_id);
            jsonWriter.name("code").value(tx.code.ordinal());
            jsonWriter.endObject();
        }

        @Override
        public FinalizeTxInfo read(JsonReader jsonReader) throws IOException {
            FinalizeTxInfo tx = new FinalizeTxInfo();
            jsonReader.beginObject();
            while (jsonReader.hasNext()){
                switch (jsonReader.nextName()){
                    case "tx_id":
                        tx.tx_id = jsonReader.nextString();
                        break;
                    case "code":
                        tx.code = ServerTxStatus.values()[jsonReader.nextInt()];
                        break;
                }
            }
            jsonReader.endObject();
            return tx;
        }
    }
}
