#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include "com_vcashorg_vcashwallet_wallet_NativeSecp256k1.h"
#include "secp256k1_bulletproofs.h"
#include "secp256k1_aggsig.h"
#include <android/log.h>
#include "blake2.h"

#define SECRET_KEY_SIZE 32
#define PEDERSEN_COMMITMENT_SIZE 33
#define PUBKEY_SIZE 64
#define COMPRESSED_PUBKEY_SIZE 33
#define MAX_WIDTH (1 << 20)
#define SCRATCH_SPACE_SIZE (256 * MAX_WIDTH)
#define MAX_PROOF_SIZE 5134
#define BULLET_PROOF_MSG_SIZE 16

#define TAG    "------vwallet jni"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)


void createRandomBytesOfLength(char* buf, size_t length) {
    FILE *fp = fopen("/dev/urandom", "r");
    if (!fp)
    {
        exit(-1);
        LOGD("-----cannot open /dev/random");
        return;
    }
    for (int i = 0; i < length; i++)
    {
        char c = fgetc(fp);
        buf[i] = c;
    }

    fclose(fp);
    return;
}

unsigned char* ConvertJByteaArrayToUnsingedChars(JNIEnv *env, jbyteArray bytearray)
{
    unsigned char *chars = NULL;
    if (bytearray != nullptr){
        jbyte *bytes = env->GetByteArrayElements(bytearray, 0);
        int chars_len = env->GetArrayLength(bytearray);
        chars = new unsigned char[chars_len];
        memcpy(chars, bytes, chars_len);

        env->ReleaseByteArrayElements(bytearray, bytes, 0);
    }

    return chars;
}

jbyteArray ConvertUnsignedCharsToJByteArray(JNIEnv *env, unsigned char *chars, uint64_t length){
    jbyte *by = (jbyte*)chars;
    jbyteArray jarray = env->NewByteArray(length);
    env->SetByteArrayRegion(jarray, 0, length, by);
    return jarray;
}

int getArrayLength(JNIEnv *env, jobjectArray array){
    int size = 0;
    if (array != nullptr){
        size = env->GetArrayLength(array);
    }
    return size;
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

secp256k1_pedersen_commitment* parseCommit(JNIEnv *env, secp256k1_context* context, jbyteArray commitment){
    unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, commitment);
    secp256k1_pedersen_commitment* innerCommit = new secp256k1_pedersen_commitment;
    int ret = secp256k1_pedersen_commitment_parse(context,
                                                  innerCommit,
                                                  bytes);
    delete bytes;
    if (ret == 1){
        return innerCommit;
    }
    else{
        return nullptr;
    }
}

secp256k1_bulletproof_generators *sharedGenerators(secp256k1_context* context){
    static secp256k1_bulletproof_generators * proofGen = NULL;
    if (proofGen == NULL) {
        proofGen = secp256k1_bulletproof_generators_create(context, &secp256k1_generator_const_g, 256);
    }

    return proofGen;
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

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1bind_1sum
        (JNIEnv *env, jobject claseObject, jlong context, jobjectArray positive, jobjectArray negative){

    int positiveSize = getArrayLength(env, positive);
    int negativeSize = getArrayLength(env, negative);
    int totalCount = positiveSize + negativeSize;
    const uint8_t * point[totalCount];
    for (int i=0; i<positiveSize; i++){
        jbyteArray byteArr = (jbyteArray)env->GetObjectArrayElement(positive,i);
        unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, byteArr);
        point[i] = bytes;
    }

    for (int i=0; i<negativeSize; i++){
        jbyteArray byteArr = (jbyteArray)env->GetObjectArrayElement(negative,i);
        unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, byteArr);
        point[positiveSize+i] = bytes;
    }

    uint8_t retData[SECRET_KEY_SIZE];
    int ret = secp256k1_pedersen_blind_sum((secp256k1_context*)(uintptr_t)context,
                                           retData,
                                           point,
                                           totalCount,
                                           positiveSize);
    for (int i=0; i<totalCount; i++){
        const uint8_t *item = point[i];
        delete item;
    }
    if (ret == 1){
        return ConvertUnsignedCharsToJByteArray(env, retData, SECRET_KEY_SIZE);
    }
    else{
        return nullptr;
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1commit_1sum
        (JNIEnv *env, jobject claseObject, jlong context, jobjectArray positive, jobjectArray negative){
    int positiveSize = getArrayLength(env, positive);
    int negativeSize = getArrayLength(env, negative);
    secp256k1_pedersen_commitment innerCommit;
    secp256k1_pedersen_commitment* positiveVec[positiveSize];
    secp256k1_pedersen_commitment* negativeVec[negativeSize];
    for (int i=0; i<positiveSize; i++){
        jbyteArray byteArr = (jbyteArray)env->GetObjectArrayElement(positive,i);
        positiveVec[i] = parseCommit(env, (secp256k1_context*)(uintptr_t)context, byteArr);
    }
    for (int i=0; i<negativeSize; i++){
        jbyteArray byteArr = (jbyteArray)env->GetObjectArrayElement(negative,i);
        negativeVec[i] = parseCommit(env, (secp256k1_context*)(uintptr_t)context, byteArr);
    }
    int ret = secp256k1_pedersen_commit_sum((secp256k1_context*)(uintptr_t)context,
                                            &innerCommit,
                                            positiveVec,
                                            positiveSize,
                                            negativeVec,
                                            negativeSize);
    if (ret == 1){
        return serCommit(env, (secp256k1_context*)(uintptr_t)context, &innerCommit);
    }
    else{
        return nullptr;
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1commit_1to_1pubkey
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray commit){
    secp256k1_pubkey pubkey;
    secp256k1_pedersen_commitment* innerCommit = parseCommit(env, (secp256k1_context*)(uintptr_t)context, commit);
    if (innerCommit){
        int ret = secp256k1_pedersen_commitment_to_pubkey((secp256k1_context*)(uintptr_t)context,
                                                          &pubkey,
                                                          innerCommit);
        if (ret == 1){
            return ConvertUnsignedCharsToJByteArray(env, pubkey.data, PUBKEY_SIZE);
        }
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1get_1compressed_1pubkey
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray pubkey){
    int pubkeyLen = env->GetArrayLength(pubkey);
    if (pubkeyLen == PUBKEY_SIZE){
        uint8_t* pubkeys = ConvertJByteaArrayToUnsingedChars(env, pubkey);
        uint8_t compressKey[COMPRESSED_PUBKEY_SIZE];
        size_t length = COMPRESSED_PUBKEY_SIZE;
        int ret = secp256k1_ec_pubkey_serialize((secp256k1_context*)(uintptr_t)context,
                                                compressKey,
                                                &length,
                                                (const secp256k1_pubkey*)pubkeys,
                                                SECP256K1_EC_COMPRESSED);
        delete pubkeys;
        if (ret == 1 && length == COMPRESSED_PUBKEY_SIZE){
            return ConvertUnsignedCharsToJByteArray(env, compressKey, COMPRESSED_PUBKEY_SIZE);
        }
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1pubkey_1from_1compressed_1key
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray compressedkey){
    int pubkeyLen = env->GetArrayLength(compressedkey);
    if (pubkeyLen == COMPRESSED_PUBKEY_SIZE){
        uint8_t* comPubkeys = ConvertJByteaArrayToUnsingedChars(env, compressedkey);
        secp256k1_pubkey pubkey;
        int ret = secp256k1_ec_pubkey_parse((secp256k1_context*)(uintptr_t)context,
                                            &pubkey,
                                            comPubkeys,
                                            COMPRESSED_PUBKEY_SIZE);
        delete comPubkeys;
        if (ret == 1){
            return ConvertUnsignedCharsToJByteArray(env, pubkey.data, PUBKEY_SIZE);
        }
    }

    return nullptr;
}

JNIEXPORT jboolean JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1verify_1single_1signature
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray signature, jbyteArray pubkey, jbyteArray nonce_sum, jbyteArray pubkey_sum, jbyteArray msg){
    uint8_t* sigs = ConvertJByteaArrayToUnsingedChars(env, signature);
    uint8_t* pubkeys = ConvertJByteaArrayToUnsingedChars(env, pubkey);
    uint8_t* nonce_sums = ConvertJByteaArrayToUnsingedChars(env, nonce_sum);
    uint8_t* pubkey_sums = ConvertJByteaArrayToUnsingedChars(env, pubkey_sum);
    uint8_t* msgs = ConvertJByteaArrayToUnsingedChars(env, msg);
    int ret = secp256k1_aggsig_verify_single((secp256k1_context*)(uintptr_t)context,
                                             sigs,
                                             msgs,
                                             (const secp256k1_pubkey *)nonce_sums,
                                             (const secp256k1_pubkey *)pubkeys,
                                             (const secp256k1_pubkey *)pubkey_sums,
                                             nullptr,
                                             true);

    delete sigs;
    delete pubkeys;
    delete nonce_sums;
    delete pubkey_sums;
    delete msgs;
    if (ret == 1){
        return true;
    }

    return false;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1calculate_1single_1signature
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray secKey, jbyteArray secNounce, jbyteArray nounceSum, jbyteArray pubkeySum, jbyteArray msg){
    uint8_t retSig[64];
    uint8_t* secKeys = ConvertJByteaArrayToUnsingedChars(env, secKey);
    uint8_t* secNounces = ConvertJByteaArrayToUnsingedChars(env, secNounce);
    uint8_t* nounceSums = ConvertJByteaArrayToUnsingedChars(env, nounceSum);
    uint8_t* pubkeySums = ConvertJByteaArrayToUnsingedChars(env, pubkeySum);
    uint8_t* msgs = ConvertJByteaArrayToUnsingedChars(env, msg);
    char seed[32];
    createRandomBytesOfLength(seed, 32);

    int ret = secp256k1_aggsig_sign_single((secp256k1_context*)(uintptr_t)context,
                                           retSig,
                                           msgs,
                                           secKeys,
                                           secNounces,
                                           nullptr,
                                           (const secp256k1_pubkey *)nounceSums,
                                           (const secp256k1_pubkey *)nounceSums,
                                           (const secp256k1_pubkey *)pubkeySums,
                                           (unsigned char*)seed);
    delete secKeys;
    delete secNounces;
    delete nounceSums;
    delete pubkeySums;
    delete msgs;
    if (ret == 1){
        return ConvertUnsignedCharsToJByteArray(env, retSig, 64);
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1combination_1pubkey
        (JNIEnv *env, jobject claseObject, jlong context, jobjectArray pubkeyArr){
    int pubkeySize = getArrayLength(env, pubkeyArr);
    secp256k1_pubkey* inkeyArr[pubkeySize];
    for (int i=0; i<pubkeySize; i++){
        jbyteArray byteArr = (jbyteArray)env->GetObjectArrayElement(pubkeyArr,i);
        unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, byteArr);
        inkeyArr[i] = (secp256k1_pubkey*)bytes;
    }
    secp256k1_pubkey outKey;
    int ret = secp256k1_ec_pubkey_combine((secp256k1_context*)(uintptr_t)context,
                                          &outKey,
                                          inkeyArr,
                                          pubkeySize);
    for (int i=0; i<pubkeySize; i++) {
        const uint8_t *item = (const uint8_t *)inkeyArr[i];
        delete item;
    }
    if (ret == 1){
        return ConvertUnsignedCharsToJByteArray(env, outKey.data, 64);
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1combination_1signature_1and_1nonceSum
        (JNIEnv *env, jobject claseObject, jlong context, jobjectArray sigArr, jbyteArray nonceSum){
    int sigSize = getArrayLength(env, sigArr);
    uint8_t* sigs[sigSize];
    for (int i=0; i<sigSize; i++){
        jbyteArray byteArr = (jbyteArray)env->GetObjectArrayElement(sigArr,i);
        unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, byteArr);
        sigs[i] = bytes;
    }
    uint8_t* nonceSums = ConvertJByteaArrayToUnsingedChars(env, nonceSum);
    uint8_t retSig[64];
    int ret = secp256k1_aggsig_add_signatures_single((secp256k1_context*)(uintptr_t)context,
                                                     retSig,
                                                     (const unsigned char**)sigs,
                                                     sigSize,
                                                     (const secp256k1_pubkey*)nonceSums);
    for (int i=0; i<sigSize; i++) {
        const uint8_t *item = sigs[i];
        delete item;
    }
    delete nonceSums;
    if (ret == 1){
        return ConvertUnsignedCharsToJByteArray(env, retSig, 64);
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1signature_1to_1compactData
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray signature){
    int sigSize = env->GetArrayLength(signature);
    if (sigSize == 64){
        uint8_t compactData[64];
        unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, signature);
        int ret = secp256k1_ecdsa_signature_serialize_compact((secp256k1_context*)(uintptr_t)context,
                                                              compactData,
                                                              (const secp256k1_ecdsa_signature*)bytes);
        delete bytes;
        if (ret == 1){
            return ConvertUnsignedCharsToJByteArray(env, compactData, 64);
        }
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1compact_1data_1to_1signature
        (JNIEnv * env, jobject claseObject, jlong context, jbyteArray compactData){
    int comSize = env->GetArrayLength(compactData);
    if (comSize == 64){
        secp256k1_ecdsa_signature retSig;
        unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, compactData);
        int ret = secp256k1_ecdsa_signature_parse_compact((secp256k1_context*)(uintptr_t)context,
                                                              &retSig,
                                                              bytes);
        delete bytes;
        if (ret == 1){
            return ConvertUnsignedCharsToJByteArray(env, retSig.data, 64);
        }
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1export_1secnonce_1single
        (JNIEnv *env, jobject claseObject, jlong context){
    char seed[32];
    createRandomBytesOfLength(seed, 32);
    uint8_t retData[32];
    int ret = secp256k1_aggsig_export_secnonce_single((secp256k1_context*)(uintptr_t)context,
                                                      retData,
                                                      (unsigned char*)seed);
    if (ret == 1){
        return ConvertUnsignedCharsToJByteArray(env, retData, 32);
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1get_1pubkey_1from_1secretKey
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray seckey){
    secp256k1_pubkey pubkey;
    unsigned char* bytes = ConvertJByteaArrayToUnsingedChars(env, seckey);
    int ret = secp256k1_ec_pubkey_create((secp256k1_context*)(uintptr_t)context,
                                         &pubkey,
                                         bytes);
    delete bytes;
    if (ret == 1){
        return ConvertUnsignedCharsToJByteArray(env, pubkey.data, PUBKEY_SIZE);
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1createbullet_1proof
        (JNIEnv *env, jobject claseObject, jlong context, jlong value, jbyteArray key, jbyteArray nounce, jbyteArray msg){
    unsigned char* keys = ConvertJByteaArrayToUnsingedChars(env, key);
    unsigned char* nounces = ConvertJByteaArrayToUnsingedChars(env, nounce);
    unsigned char* msgs = ConvertJByteaArrayToUnsingedChars(env, msg);

    uint8_t proof[MAX_PROOF_SIZE];
    size_t proofSize = MAX_PROOF_SIZE;
    uint64_t amount = value;
    const unsigned char* const* key_points = &keys;
    //const unsigned char* const* temp = &((const unsigned char*)key.data.bytes);

    secp256k1_scratch_space* scratch = secp256k1_scratch_space_create((secp256k1_context*)(uintptr_t)context, SCRATCH_SPACE_SIZE);
    int ret = secp256k1_bulletproof_rangeproof_prove((secp256k1_context*)(uintptr_t)context,
                                                     scratch,
                                                     sharedGenerators((secp256k1_context*)(uintptr_t)context),
                                                     proof,
                                                     &proofSize,
                                                     NULL,
                                                     NULL,
                                                     NULL,
                                                     &amount,
                                                     NULL,
                                                     key_points,
                                                     NULL,
                                                     1,
                                                     &secp256k1_generator_const_h,
                                                     64,
                                                     nounces,
                                                     NULL,
                                                     NULL,
                                                     0,
                                                     msgs);
    secp256k1_scratch_space_destroy(scratch);
    delete keys;
    delete nounces;
    delete msgs;

    if (ret == 1){
        return ConvertUnsignedCharsToJByteArray(env, proof, proofSize);
    }

    return nullptr;
}

JNIEXPORT jboolean JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1verify_1bullet_1proof
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray commit, jbyteArray proof){
    unsigned char* proofs = ConvertJByteaArrayToUnsingedChars(env, proof);
    int proofLen = env->GetArrayLength(proof);
    secp256k1_scratch_space* scratch = secp256k1_scratch_space_create((secp256k1_context*)(uintptr_t)context, SCRATCH_SPACE_SIZE);
    int ret = secp256k1_bulletproof_rangeproof_verify((secp256k1_context*)(uintptr_t)context,
                                                      scratch,
                                                      sharedGenerators((secp256k1_context*)(uintptr_t)context),
                                                      proofs,
                                                      proofLen,
                                                      NULL,
                                                      parseCommit(env, (secp256k1_context*)(uintptr_t)context, commit),
                                                      1,
                                                      64,
                                                      &secp256k1_generator_const_h,
                                                      NULL,
                                                      0);
    secp256k1_scratch_space_destroy(scratch);
    delete proofs;
    if (ret == 1){
        return true;
    }

    return false;
}

JNIEXPORT jobject JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1rewind_1bullet_1proof
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray commitment, jbyteArray nounce, jbyteArray proof){
    uint8_t blindOut[SECRET_KEY_SIZE];
    uint64_t value = 0;
    uint8_t messageOut[BULLET_PROOF_MSG_SIZE];

    unsigned char* proofs = ConvertJByteaArrayToUnsingedChars(env, proof);
    int proofLen = env->GetArrayLength(proof);
    unsigned char* nounces = ConvertJByteaArrayToUnsingedChars(env, nounce);

    int ret = secp256k1_bulletproof_rangeproof_rewind((secp256k1_context*)(uintptr_t)context,
                                                      sharedGenerators((secp256k1_context*)(uintptr_t)context),
                                                      &value,
                                                      blindOut,
                                                      proofs,
                                                      proofLen,
                                                      0,
                                                      parseCommit(env, (secp256k1_context*)(uintptr_t)context, commitment),
                                                      &secp256k1_generator_const_h,
                                                      nounces,
                                                      NULL,
                                                      0,
                                                      messageOut);
    delete proofs;
    delete nounces;
    if (ret == 1){
        jclass proofClass = env->FindClass("com/vcashorg/vcashwallet/wallet/WallegtType/VcashProofInfo");
        jmethodID id_generate=env->GetMethodID(proofClass,"<init>", "()V");
        jobject proof=env->NewObject(proofClass,id_generate);
        jfieldID proofValue = env->GetFieldID(proofClass,"value","J");
        jfieldID proofMsg = env->GetFieldID(proofClass,"msg","[B");
        env->SetLongField(proof, proofValue, value);

        jbyte *by = (jbyte*)messageOut;
        jbyteArray jarray = env->NewByteArray(BULLET_PROOF_MSG_SIZE);
        env->SetByteArrayRegion(jarray, 0, BULLET_PROOF_MSG_SIZE, by);
        env->SetObjectField(proof,proofMsg,jarray);

        return proof;
    }

    return nullptr;
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1ecdsa_1sign
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray msgdata, jbyteArray seckey){
    unsigned char* msg = ConvertJByteaArrayToUnsingedChars(env, msgdata);
    size_t msgLength = env->GetArrayLength(msgdata);
    unsigned char* secret = ConvertJByteaArrayToUnsingedChars(env, seckey);
    secp256k1_ecdsa_signature sig;
    uint8_t hash[32];
    if( blake2b( hash, msg, nullptr, 32, msgLength, 0 ) < 0){
        return nullptr;
    }
    int ret = secp256k1_ecdsa_sign((secp256k1_context*)(uintptr_t)context,
                                   &sig,
                                   hash,
                                   secret,
                                   NULL,
                                   NULL);
    delete msg;
    delete secret;
    if (ret == 1){
        jbyteArray sigData = ConvertUnsignedCharsToJByteArray(env, sig.data, 64);
        return Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1signature_1to_1compactData(env, claseObject, context, sigData);
    }

    return nullptr;
}

JNIEXPORT jboolean JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1ecdsa_1verify
        (JNIEnv *env, jobject claseObject, jlong context, jbyteArray msgdata, jbyteArray compactSig, jbyteArray pubkeyData){
    unsigned char* msg = ConvertJByteaArrayToUnsingedChars(env, msgdata);
    size_t msgLength = env->GetArrayLength(msgdata);
    uint8_t hash[32];
    if( blake2b( hash, msg, nullptr, 32, msgLength, 0 ) < 0){
        return false;
    }

    jbyteArray sigData = Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_secp256k1_1compact_1data_1to_1signature(env, claseObject, context, compactSig);
    unsigned char* signature = ConvertJByteaArrayToUnsingedChars(env, sigData);

    secp256k1_pubkey pubkey;
    unsigned char* pubkey_data = ConvertJByteaArrayToUnsingedChars(env, pubkeyData);
    size_t pubkeyLength = env->GetArrayLength(pubkeyData);
    int ret = secp256k1_ec_pubkey_parse((secp256k1_context*)(uintptr_t)context,
                                        &pubkey,
                                        pubkey_data,
                                        pubkeyLength);
    if (ret == 1){
        ret = secp256k1_ecdsa_verify((secp256k1_context*)(uintptr_t)context,
                                     (secp256k1_ecdsa_signature*)signature,
                                     hash,
                                     &pubkey);
    }
    delete signature;
    delete msg;
    delete pubkey_data;

    return (ret == 1);
}

JNIEXPORT jbyteArray JNICALL Java_com_vcashorg_vcashwallet_wallet_NativeSecp256k1_blake_12b
        (JNIEnv *env, jobject claseObject, jbyteArray inputData, jbyteArray keyData){
    uint8_t retData[32];
    unsigned char* input = ConvertJByteaArrayToUnsingedChars(env, inputData);
    size_t inputLength = env->GetArrayLength(inputData);
    unsigned char* key = ConvertJByteaArrayToUnsingedChars(env, keyData);
    size_t keyLength = 0;
    if (key != nullptr){
        keyLength = env->GetArrayLength(keyData);
    }
    int ret = blake2b(retData, input, key, 32, inputLength, keyLength);
    delete input;
    delete key;
    if (ret >= 0){
        return ConvertUnsignedCharsToJByteArray(env, retData, 32);
    }

    return nullptr;
}