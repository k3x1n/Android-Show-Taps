#include <jni.h>
#include <string>
#include <android/log.h>
#include <linux/input.h>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>

#include "detect_devices.h"

#define LOG_TAG "k3x1n"

#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static const int MAX_POINTER_COUNT = 10;


jobject getInputPath(JNIEnv *env, jclass) {
    LOGD("uid = %d", getuid());

    auto listClazz = env->FindClass("java/util/ArrayList");
    auto listInit = env->GetMethodID(listClazz, "<init>", "()V");
    auto listResult = env->NewObject(listClazz, listInit);

    scan_dir(env, listResult);

    return listResult;
}

int volatile runningSync = 0;

int volatile fd_dev = 0;

bool volatile wait_thread_start = false;

static int slot_pressed[MAX_POINTER_COUNT];
static float slot_x[MAX_POINTER_COUNT];
static float slot_y[MAX_POINTER_COUNT];

int slot = 0;

jobject nativeLibClazz = nullptr;
jmethodID methodA = nullptr;
jmethodID methodB = nullptr;
jmethodID methodC = nullptr;
JavaVM* gJavaVm;


void* reader(void*){
    int fd = fd_dev;

    struct input_absinfo abs_width;
    struct input_absinfo abs_height;
    auto res2 = ioctl(fd, EVIOCGABS(ABS_MT_POSITION_X), &abs_width);
    auto res3 = ioctl(fd, EVIOCGABS(ABS_MT_POSITION_Y), &abs_height);
    if(res2 != 0 || res3 != 0){
        LOGD("res2 = %d, res3 = %d", res2, res3);
        wait_thread_start = false;
        return nullptr;
    }
    if(abs_width.maximum == 0){
        abs_width.maximum = 1;
    }
    if(abs_width.minimum == 0){
        abs_width.minimum = 1;
    }

    void* eventPtr = malloc(sizeof(input_event));

    JNIEnv *env = nullptr;
    auto isAttach = gJavaVm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if(isAttach != JNI_OK){
        LOGD("%s", "!isAttach");
        gJavaVm->AttachCurrentThread(&env, nullptr);
        auto isAttach2 = gJavaVm->GetEnv((void**)&env, JNI_VERSION_1_6);
        LOGD("isAttach = %d, isAttach2 = %d", isAttach, isAttach2);
    }

    if(env == nullptr){
        LOGD("%s", "env == nullptr !!!");
        close(fd);
        return nullptr;
    }

    for(int i = 0 ; i < MAX_POINTER_COUNT ; i++){
        slot_pressed[i] = false;
    }

    int sync = runningSync;
    wait_thread_start = false;
    while(sync == runningSync){
        auto res = read(fd, eventPtr, sizeof (input_event));
        auto event = (input_event*)eventPtr;

        if(event->type == EV_ABS){
            if(event->code == ABS_MT_TRACKING_ID){
                LOGD("ABS_MT_TRACKING_ID %d", event->value);
                if(slot < MAX_POINTER_COUNT){
                    slot_pressed[slot] = event->value != -1;
                }

            }else if(event->code == ABS_MT_SLOT){
                LOGD("ABS_MT_SLOT %d", event->value);
                slot = event->value;
                slot_pressed[slot] = true;

            }else if(event->code == ABS_MT_POSITION_X){
                if(slot < MAX_POINTER_COUNT){
                    slot_x[slot] = 1.0f * event->value / abs_width.maximum;
                }
            }else if(event->code == ABS_MT_POSITION_Y){
                if(slot < MAX_POINTER_COUNT){
                    slot_y[slot] = 1.0f * event->value / abs_height.maximum;
                }
            }
        }else if(event->type == EV_SYN){
            for(int i = 0 ; i < MAX_POINTER_COUNT ; i++){
                env->CallStaticVoidMethod((jclass)nativeLibClazz,
                                          methodA, i, slot_pressed[i]);
                if(slot_pressed[i] == true){
                    env->CallStaticVoidMethod((jclass)nativeLibClazz,
                                              methodB, i, slot_x[i], slot_y[i]);
                }
            }
            env->CallStaticVoidMethod((jclass)nativeLibClazz, methodC);
        }
    }

    if(isAttach != JNI_OK){
        gJavaVm->DetachCurrentThread();
    }

    close(fd);

    return nullptr;
}

jboolean start(JNIEnv *env, jclass, jstring dev) {
    auto chars = env->GetStringUTFChars(dev, JNI_FALSE);
    fd_dev = open(chars, O_RDONLY);
    env->ReleaseStringUTFChars(dev, chars);

    LOGD("fd = %d, errno = %d", fd_dev, errno);

    if(fd_dev <= 0){
        return false;
    }

    pthread_t tid;
    wait_thread_start = true;
    runningSync++;
    int code = pthread_create(&tid, nullptr, reader, nullptr);
    if (code != 0){
        wait_thread_start = false;
        LOGD("pthread_create err = %d", code);
        return false;
    }

    while(wait_thread_start){
        // sleep 50ms:
        usleep(1000 * 50);
    }

    return true;
}

void stop(JNIEnv *, jclass) {
    runningSync++;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*) {
    gJavaVm = vm;
    JNIEnv *env;
    vm->GetEnv((void **) (&env), JNI_VERSION_1_6);

    auto clazz = env->FindClass("show/taps/server/NativeLib");
    methodA = env->GetStaticMethodID(clazz, "a", "(IZ)V");
    methodB = env->GetStaticMethodID(clazz, "b", "(IFF)V");
    methodC = env->GetStaticMethodID(clazz, "c", "()V");
    nativeLibClazz = env->NewGlobalRef(clazz);

    static const JNINativeMethod methods[] = {
        {"start", "(Ljava/lang/String;)Z", (void*) start},
        {"getInputPath", "()Ljava/util/ArrayList;", (void*) getInputPath},
        {"stop", "()V", (void*) stop},
    };

    auto mMethods = sizeof(methods) / sizeof(JNINativeMethod);
    env->RegisterNatives(clazz, methods, (jint)mMethods);

    return JNI_VERSION_1_6;
}
