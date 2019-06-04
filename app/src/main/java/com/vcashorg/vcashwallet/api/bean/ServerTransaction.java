package com.vcashorg.vcashwallet.api.bean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.Serializable;

public class ServerTransaction implements Serializable {
    public String tx_id;
    public String sender_id;
    public String receiver_id;
    public String slate;
    public ServerTxStatus status;

    @Expose(serialize = false, deserialize = false)
    public VcashSlate slateObj;
    @Expose(serialize = false, deserialize = false)
    public boolean isSend;

    public ServerTransaction(VcashSlate sla){
        tx_id = sla.uuid;
        Gson gson = new GsonBuilder().registerTypeAdapter(VcashSlate.class, sla.new VcashSlateTypeAdapter()).create();
        slate = gson.toJson(sla);
        slate = StringEscapeUtils.unescapeJson(slate);
    }

    private ServerTransaction(){

    }

    public class ServerTransactionTypeAdapter extends TypeAdapter<ServerTransaction> {
        @Override
        public void write(JsonWriter jsonWriter, ServerTransaction tx) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name("tx_id").value(tx.tx_id);
            jsonWriter.name("sender_id").value(tx.sender_id);
            jsonWriter.name("receiver_id").value(tx.receiver_id);
            jsonWriter.name("slate").value(tx.slate);
            jsonWriter.name("status").value(tx.status.ordinal());
            jsonWriter.endObject();
        }

        @Override
        public ServerTransaction read(JsonReader jsonReader) throws IOException {
            ServerTransaction tx = new ServerTransaction();
            jsonReader.beginObject();
            while (jsonReader.hasNext()){
                switch (jsonReader.nextName()){
                    case "tx_id":
                        tx.tx_id = jsonReader.nextString();
                        break;
                    case "sender_id":
                        tx.sender_id = jsonReader.nextString();
                        break;
                    case "receiver_id":
                        tx.receiver_id = jsonReader.nextString();
                        break;
                    case "slate":
                        tx.slate = jsonReader.nextString();
                        break;
                    case "status":
                        tx.status = ServerTxStatus.values()[jsonReader.nextInt()];
                        break;
                }
            }
            jsonReader.endObject();
            return tx;
        }
    }
}
