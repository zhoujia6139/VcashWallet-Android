package com.vcashorg.vcashwallet.wallet;

import android.util.Log;

import com.vcashorg.vcashwallet.utils.AppUtil;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.BIG_ENDIAN;

public class PaymentProof {

    public static String createUUID() {
        return create_random_uuid();
    }

    public static byte[] bytesFromUUID(String uuid) {
        String hex = bytes_from_uuid(uuid);
        return AppUtil.decode(hex);
    }

    public static String UUIDFromBytes(byte[] bytes) {
        String hex = AppUtil.hex(bytes);
        return uuid_from_bytes(hex);
    }

    public static String createSlatePackMsg(byte[] slate, String receiverAddr, String senderAddr) {
        String hex = AppUtil.hex(slate);
        String slatePack = create_slatepack_message(hex, receiverAddr, senderAddr);
        return slatePack;
    }

    public static byte[] slateFromSlatePackMessage(String slatePack, String privateKey) {
        String hex = slate_from_slatepack_message(slatePack, privateKey);
        return AppUtil.decode(hex);
    }

    public static String senderAddrFromSlatePackMessage(String slatePack, String privateKey) {
        String addr = sender_address_from_slatepack_message(slatePack, privateKey);
        return addr;
    }

    public static String getPaymentProofAddress(byte[] privateKey) {
        return get_payment_proof_address(AppUtil.hex(privateKey), AppUtil.isInTestNet);
    }

    public static String getPubkeyFromProofAddress(String proofAddress) {
        if (proofAddress == null) {
            return null;
        }
        if (proofAddress.length() != 65 && proofAddress.length() != 64) {
            return null;
        }
        return get_pubkey_from_proof_address(proofAddress);
    }

    public static String getProofAddressFromPubkey(String pubkey) {
        return get_proof_address_from_pubkey(pubkey, AppUtil.isInTestNet);
    }

    public static String createPaymentProofSignature(String tokenType, long amount, String excess, String senderPubkey, String privateKey) {
        String msg = getPaymentProofMessage(tokenType, amount, excess, senderPubkey);
        return create_payment_proof_signature(msg, privateKey);
    }

    public static String createSignature(String msg, String privateKey) {
        return create_payment_proof_signature(msg, privateKey);
    }

    public static boolean verifyPaymentProof(String tokenType, long amount, String excess, String senderPubkey, String receiverPubkey, String signature) {
        String msg = getPaymentProofMessage(tokenType, amount, excess, senderPubkey);
        return verify_payment_proof(msg, receiverPubkey, signature);
    }

    public static boolean verifySignature(String msg, String pubkey, String signature){
        return verify_payment_proof(msg, pubkey, signature);
    }

    private static String getPaymentProofMessage(String tokenType, long amount, String excess, String senderPubkey) {
        int length = 1024;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(BIG_ENDIAN);
        if (tokenType != null) {
            buf.put(AppUtil.decode(tokenType));
        }
        buf.putLong(amount);
        buf.put(AppUtil.decode(excess));
        buf.put(AppUtil.decode(senderPubkey));
        buf.flip();
        byte[] data = AppUtil.BufferToByteArr(buf);
        return AppUtil.hex(data);
    }

    private static native String get_payment_proof_address(String privateKey, boolean isTest);

    private static native String get_pubkey_from_proof_address(String proofAddress);

    private static native String get_proof_address_from_pubkey(String pubkey, boolean isTest);

    private static native String create_payment_proof_signature(String msg, String privateKey);

    private static native boolean verify_payment_proof(String msg, String pubKey, String signature);

    private static native String create_slatepack_message(String slateStr, String receiverAddr, String senderAddr);

    private static native String slate_from_slatepack_message(String slatePack, String privateKey);

    private static native String sender_address_from_slatepack_message(String slatePack, String privateKey);

    private static native String create_random_uuid();

    private static native String bytes_from_uuid(String uuid);

    private static native String uuid_from_bytes(String hexBytes);
}
