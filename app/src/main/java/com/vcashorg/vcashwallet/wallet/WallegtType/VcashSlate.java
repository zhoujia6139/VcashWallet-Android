package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class VcashSlate {
    @SerializedName("id")
    public String uuid;
    public short num_participants;
    public long amount;
    public long fee;
    public long height;
    public long lock_height;
    @SerializedName("version")
    public long slate_version;
    public VcashTransaction tx;
    public ArrayList<ParticipantData> participant_data;


    public class ParticipantData{
        @SerializedName("id")
        public short pId;
        public byte[] public_blind_excess;
        public byte[] public_nonce;
        public byte[] part_sig;
        public String message;
        public byte[] message_sig;
    }
}
