package com.vcashorg.vcashwallet.api.bean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;
import com.vcashorg.vcashwallet.wallet.PaymentProof;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashSlate;
import com.vcashorg.vcashwallet.wallet.WalletApi;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class ServerTransaction implements Serializable {
    public String tx_id;
    public String sender_id;
    public String receiver_id;
    public String slate;
    public ServerTxStatus status;
    public String msg_sig;
    public String tx_sig;

    @Expose(serialize = false, deserialize = false)
    public VcashSlate slateObj;
    @Expose(serialize = false, deserialize = false)
    public boolean isSend;

    public ServerTransaction(VcashSlate sla){
        tx_id = sla.uuid;
        slate = WalletApi.encryptSlateForParter(sla);
    }

    public ServerTransaction(){

    }

    public byte[] msgToSign(){
        ByteBuffer buf = ByteBuffer.allocate(100);
        String short_tx_id = tx_id.replace("-", "");
        buf.put(AppUtil.decode(short_tx_id));
        String senderPubkey = WalletApi.getPubkeyFromProofAddress(sender_id);
        buf.put(AppUtil.decode(senderPubkey));
        String receiverPubkey = WalletApi.getPubkeyFromProofAddress(receiver_id);
        buf.put(AppUtil.decode(receiverPubkey));
        buf.flip();
        return AppUtil.BufferToByteArr(buf);
    }

    public static class ServerTransactionTypeAdapter extends TypeAdapter<ServerTransaction> {
        @Override
        public void write(JsonWriter jsonWriter, ServerTransaction tx) throws IOException {
            jsonWriter.beginObject();
            jsonWriter.name("tx_id").value(tx.tx_id);
            jsonWriter.name("sender_id").value(PaymentProof.getPubkeyFromProofAddress(tx.sender_id));
            jsonWriter.name("receiver_id").value(PaymentProof.getPubkeyFromProofAddress(tx.receiver_id));
            jsonWriter.name("slate").value(tx.slate);
            jsonWriter.name("status").value(tx.status.code());
            jsonWriter.name("msg_sig").value(tx.msg_sig);
            jsonWriter.name("tx_sig").value(tx.tx_sig);
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
                        String sender_id = jsonReader.nextString();
                        tx.sender_id = PaymentProof.getProofAddressFromPubkey(sender_id);
                        break;
                    case "receiver_id":
                        String receiver_id = jsonReader.nextString();
                        tx.receiver_id = PaymentProof.getProofAddressFromPubkey(receiver_id);
                        break;
                    case "slate":
                        tx.slate = jsonReader.nextString();
                        break;
                    case "status":
                        tx.status = ServerTxStatus.locateEnum(jsonReader.nextInt());
                        break;
                    case "msg_sig":
                        tx.msg_sig = jsonReader.nextString();
                        break;
                    case "tx_sig":
                        tx.tx_sig = jsonReader.nextString();
                        break;
                }
            }
            jsonReader.endObject();
            return tx;
        }
    }
}
