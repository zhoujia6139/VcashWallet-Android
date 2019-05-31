package com.vcashorg.vcashwallet.wallet.WallegtType;

import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.NativeSecp256k1;

public class VcashContext {
    public String sec_key;
    public String sec_nounce = AppUtil.hex(NativeSecp256k1.instance().exportSecnonceSingle());
    public String slate_id;
}
