package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;

import java.io.Serializable;
import java.util.ArrayList;

public class VcashContext implements Serializable {
    public String sec_key;
    public String token_sec_key;
    public String sec_nounce = AppUtil.hex(NativeSecp256k1.instance().exportSecnonceSingle());
    public String slate_id;
    public long amout;
    public long fee;
    public ArrayList<VcashCommitId> output_ids = new ArrayList<>();
    public ArrayList<VcashCommitId> input_ids = new ArrayList<>();
    public ArrayList<VcashCommitId> token_output_ids = new ArrayList<>();
    public ArrayList<VcashCommitId> token_input_ids = new ArrayList<>();
}
