/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_xjl_emp3recorder_mp3record_LameUtil */

#ifndef _Included_com_xjl_emp3recorder_mp3record_LameUtil
#define _Included_com_xjl_emp3recorder_mp3record_LameUtil
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_xjl_emp3recorder_mp3record_LameUtil
 * Method:    init
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_com_xjl_emp3recorder_mp3record_LameUtil_init
  (JNIEnv *, jclass, jint, jint, jint, jint, jint);

/*
 * Class:     com_xjl_emp3recorder_mp3record_LameUtil
 * Method:    encode
 * Signature: ([S[SI[B)I
 */
JNIEXPORT jint JNICALL Java_com_xjl_emp3recorder_mp3record_LameUtil_encode
  (JNIEnv *, jclass, jshortArray, jshortArray, jint, jbyteArray);

/*
 * Class:     com_xjl_emp3recorder_mp3record_LameUtil
 * Method:    flush
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_xjl_emp3recorder_mp3record_LameUtil_flush
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_xjl_emp3recorder_mp3record_LameUtil
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_xjl_emp3recorder_mp3record_LameUtil_close
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
