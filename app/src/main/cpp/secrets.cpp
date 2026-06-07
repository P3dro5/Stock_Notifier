#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_stockwidget_security_Secrets_getFinnhubApiKey(
        JNIEnv* env,
        jobject) {
    std::string key;
    return env->NewStringUTF(key.c_str());
}