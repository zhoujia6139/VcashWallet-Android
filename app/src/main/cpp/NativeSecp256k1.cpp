#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include "com_vcashorg_vcashwallet_wallet_NativeSecp256k1.h"
#include "secp256k1_bulletproofs.h"
#include "secp256k1_aggsig.h"

#define SECRET_KEY_SIZE 32
#define PEDERSEN_COMMITMENT_SIZE 33


unsigned char* ConvertJByteaArrayToUnsingedChars(JNIEnv *env, jbyteArray bytearray)
{
    unsigned char *chars = NULL;
    jbyte *bytes = env->GetByteArrayElements(bytearray, 0);
    int chars_len = env->GetArrayLength(bytearray);
    chars = new unsigned char[chars_len];
    memcpy(chars, bytes, chars_len);

    env->ReleaseByteArrayElements(bytearray, bytes, 0);

    return chars;
}

jbyteArray ConvertUnsignedCharsToJByteArray(JNIEnv *env, unsigned char *chars, uint64_t length){
    jbyte *by = (jbyte*)chars;
    jbyteArray jarray = env->NewByteArray(length);
    env->SetByteArrayRegion(jarray, 0, length, by);
    return jarray;
}

secp256k1_pubkey* getGeneratorJPub(){
    static secp256k1_pubkey GENERATOR_J_PUB = {{
                                                   0x5f, 0x15, 0x21, 0x36, 0x93, 0x93, 0x01, 0x2a,
                                                   0x8d, 0x8b, 0x39, 0x7e, 0x9b, 0xf4, 0x54, 0x29,
                                                   0x2f, 0x5a, 0x1b, 0x3d, 0x38, 0x85, 0x16, 0xc2,
                                                   0xf3, 0x03, 0xfc, 0x95, 0x67, 0xf5, 0x60, 0xb8,
                                                   0x3a, 0xc4, 0xc5, 0xa6, 0xdc, 0xa2, 0x01, 0x59,
                                                   0xfc, 0x56, 0xcf, 0x74, 0x9a, 0xa6, 0xa5, 0x65,
                                                   0x31, 0x6a, 0xa5, 0x03, 0x74, 0x42, 0x3f, 0x42,
                                                   0x53, 0x8f, 0xaa, 0x2c, 0xd3, 0x09, 0x3f, 0xa4
                                           }};

    return &GENERATOR_J_PUB;
}

jbyteArray serCommit(JNIEnv *env, secp256k1_context* context, secp256k1_pedersen_commitment* innercommitment){
    uint8_t outCommit[PEDERSEN_COMMITMENT_SIZE];
    secp256k1_pedersen_commitment_serialize(context,
                                            outCommit,
                                            innercommitment);
    return ConvertUnsignedCharsToJByteArray(env, outCommit, PEDERSEN_COMMITMENT_SIZE);
}

JNIEXPORT jlong JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1ctx_1create
        (JNIEnv *, jobject){
    jlong ctx = (uintptr_t)secp256k1_context_create(SECP256K1_CONTEXT_SIGN | SECP256K1_CONTEXT_VERIFY);
    return ctx;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1bind_1switch
        (JNIEnv * env, jobject classObject, jlong context, jlong value, jbyteArray key){
    unsigned char outdata[SECRET_KEY_SIZE];
    unsigned char* keyData = ConvertJByteaArrayToUnsingedChars(env, key);
    int ret = secp256k1_blind_switch((secp256k1_context*)(uintptr_t)context,
                                     outdata,
                                     keyData,
                                     value,
                                     &secp256k1_generator_const_h,
                                     &secp256k1_generator_const_g,
                                     getGeneratorJPub());
    delete keyData;
    if (ret == 1) {
        return ConvertUnsignedCharsToJByteArray(env, outdata, SECRET_KEY_SIZE);
    }

    return nullptr;
}

JNIEXPORT jboolean JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1verify_1ec_1ecretKey
        (JNIEnv *env, jobject classObject, jlong context, jbyteArray key){
    unsigned char* keyData = ConvertJByteaArrayToUnsingedChars(env, key);
    if (secp256k1_ec_seckey_verify((secp256k1_context*)(uintptr_t)context, keyData) == 1){
        delete keyData;
        return true;
    }
    delete keyData;
    return false;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1get_1commitment
        (JNIEnv *env, jobject claseObject, jlong context, jlong value, jbyteArray key){
    unsigned char* keyData = ConvertJByteaArrayToUnsingedChars(env, key);
    secp256k1_pedersen_commitment innerCommit;
    int ret = secp256k1_pedersen_commit((secp256k1_context*)(uintptr_t)context,
                                        &innerCommit,
                                        keyData,
                                        value,
                                        &secp256k1_generator_const_h,
                                        &secp256k1_generator_const_g);
    delete keyData;
    if (ret == 1){
        return serCommit(env, (secp256k1_context*)(uintptr_t)context, &innerCommit);
    }

    return nullptr;
}
