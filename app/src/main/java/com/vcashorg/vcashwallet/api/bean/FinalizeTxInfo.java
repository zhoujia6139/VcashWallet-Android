package com.vcashorg.vcashwallet.api.bean;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.utils.AppUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public class FinalizeTxInfo {
    public String tx_id;
    public ServerTxStatus code;
    public String msg_sig;

    public static class FinalizeTxInfoTypeAdapter extends TypeAdapter<FinalizeTxInfo> {
        @Override
        public void write(JsonWriter jsonWriter, FinalizeTxInfo tx) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name("tx_id").value(tx.tx_id);
            jsonWriter.name("code").value(tx.code.code());
            jsonWriter.name("msg_sig").value(tx.msg_sig);
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
                        tx.code = ServerTxStatus.locateEnum(jsonReader.nextInt());
                        break;
                    case "msg_sig":
                        tx.msg_sig = jsonReader.nextString();
                        break;
                }
            }
            jsonReader.endObject();
            return tx;
        }
    }

    public byte[] msgToSign(){
        ByteBuffer buf = ByteBuffer.allocate(100);
        String short_tx_id = tx_id.replace("-", "");
        buf.put(AppUtil.decode(short_tx_id));
        byte bit = (byte)(code.code() & 0xff);
        buf.put(bit);
        buf.flip();
        return AppUtil.BufferToByteArr(buf);
    }
}
