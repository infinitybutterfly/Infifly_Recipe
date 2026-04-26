// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("infiflyrecipe");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("infiflyrecipe")
//      }
//    }

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "NDK_RETROFIT"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_infinitybutterfly_infiflyrecipe_utils_RetrofitClient_getMealDbKey(
        JNIEnv* env,
        jobject /* this */) {
//    For Showing in Logcat when API Keys are called
    LOGI("C++ function called to fetch API Key! MyMealDB");
    // My API Key here
//    Saving My API Keys in parts for making it more harder to find
    std::string op = "https://www.themeal";
    std::string mn = "db.com/a";
    std::string dq = "pi/jso";
    std::string lp = "n/v1/1/";
//    Saving My API keys as a Single String
//    std::string api_key = "https://www.themealdb.com/api/json/v1/1/";
    return env->NewStringUTF((op + mn + dq + lp).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_infinitybutterfly_infiflyrecipe_utils_RetrofitClient_getKtorKey(
        JNIEnv* env,
        jobject /* this */) {
//    For Showing in Logcat when API Keys are called
    LOGI("C++ function called to fetch API Key! Ktor");
    std::string p = "YOUR_OWN";
    std::string t = "CREATED";
    std::string r4 = "SERVER";
    std::string a4 = "LINK";
//    std::string api_key = "https://infiflyrecipeserver.onrender.com/";
    return env->NewStringUTF((p + t + r4 + a4).c_str());
}
