package com.vcashorg.vcashwallet.wallet;

import com.vcashorg.vcashwallet.api.NodeApi;
import com.vcashorg.vcashwallet.api.bean.NodeChainInfo;
import com.vcashorg.vcashwallet.api.bean.NodeOutputs;
import com.vcashorg.vcashwallet.utils.AppUtil;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashOutput;
import com.vcashorg.vcashwallet.wallet.WallegtType.VcashWalletInfo;
import com.vcashorg.vcashwallet.wallet.WallegtType.WalletCallback;

import java.util.ArrayList;
import com.vcashorg.vcashwallet.utils.SPUtil;
import com.vcashorg.vcashwallet.utils.UIUtils;

public class VcashWallet {
    public ArrayList<VcashOutput> outputs;

    final private long DEFAULT_BASE_FEE = 1000000;
    private static VcashWallet instance = null;
    private VcashKeychain mKeyChain;
    private VcashKeychainPath mKeyPath;
    private long mChainHeight;
    private short mCurTxLogId;
    public String mUserId = null;

    private VcashWallet(VcashKeychain keychain){
        mKeyChain = keychain;
        mUserId = this.createUserId();
        SPUtil.getInstance(UIUtils.getContext()).setValue(SPUtil.USER_ID,mUserId);
    }

    public static void createVcashWallet(VcashKeychain keychain){
        if (instance == null){
            instance = new VcashWallet(keychain);
        }
    }

    public static VcashWallet getInstance(){
        return instance;
    }

    public long getChainHeight(){
        long height = NodeApi.getChainHeight(new WalletCallback() {
            @Override
            public void onCall(boolean yesOrNo, Object data) {
                NodeChainInfo info = (NodeChainInfo)data;
                if (yesOrNo && info.height > mChainHeight){
                    mChainHeight = info.height;
                    saveBaseInfo();
                }
            }
        });
        if (height > mChainHeight){
            mChainHeight = height;
        }
        return mChainHeight;
    }

    public VcashKeychainPath nextChild(){
        if (mKeyPath != null){
            mKeyPath = mKeyPath.nextPath();
            saveBaseInfo();
        }
        else{
            mKeyPath = new VcashKeychainPath(3, 0, 0, 0, 0);
        }

        return mKeyPath;
    }

    public short getNextLogId(){
        mCurTxLogId += 1;
        saveBaseInfo();
        return mCurTxLogId;
    }

    public void setChainOutputs(ArrayList<VcashOutput> chainOutputs){
        outputs = chainOutputs;
        VcashKeychainPath maxKeyPath = new VcashKeychainPath(3, 0, 0, 0, 0);
        for (VcashOutput item :outputs){
            VcashKeychainPath keyPath = new VcashKeychainPath(3, AppUtil.decode(item.keyPath));
            if (keyPath.compareTo(maxKeyPath) > 0){
                maxKeyPath = keyPath;
            }
        }
        mKeyPath = maxKeyPath;
        saveBaseInfo();
        syncOutputInfo();
    }

    public void syncOutputInfo(){

    }

    public VcashOutput identifyUtxoOutput(NodeOutputs.NodeOutput nodeOutput){
        return null;
    }

    private long calcuteFee(short inputCount, short outputCount){
        int tx_weight = outputCount*4 + 1 - inputCount;
        if (tx_weight < 1){
            tx_weight = 1;
        }

        return DEFAULT_BASE_FEE*tx_weight;
    }

    private String createUserId(){
        byte[] key = this.mKeyChain.deriveBindKey(0, new VcashKeychainPath(4, 0, 0, 0,0));
        return AppUtil.hex(key);
    }

    private void saveBaseInfo(){
        VcashWalletInfo info = new VcashWalletInfo();
        info.curHeight = mChainHeight;
        info.curKeyPath = AppUtil.hex(mKeyPath.pathData());
        info.curTxLogId = mCurTxLogId;
    }
}
