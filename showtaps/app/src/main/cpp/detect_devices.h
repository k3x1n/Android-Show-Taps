#include <jni.h>
#include <string>
#include <android/log.h>
#include <linux/input.h>
#include <dirent.h>
#include <unistd.h>
#include <fcntl.h>

#define INPUT_PATH "/dev/input/"
#define LOG_TAG "k3x1n"
#define SHOW_LOG true

#define LOGD(...)  if(SHOW_LOG) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

char* first_dev = nullptr;
char* first_dev_name = nullptr;
int first_dev_width, first_dev_height;
int dev_type, dev_weight;

static void open_device(const char *device){
    int fd = open(device, O_RDONLY | O_CLOEXEC);
    if(fd < 0) {
        LOGD("could not open %s, %s\n", device, strerror(errno));
        return;
    }

    struct input_absinfo abs_tracking_id;
    struct input_absinfo abs_width;
    struct input_absinfo abs_height;
    struct input_absinfo abs_slot;
    struct input_absinfo abs_major;
    struct input_absinfo abs_minor;

    auto res1 = ioctl(fd, EVIOCGABS(ABS_MT_TRACKING_ID), &abs_tracking_id);
    auto res2 = ioctl(fd, EVIOCGABS(ABS_MT_POSITION_X),  &abs_width);
    auto res3 = ioctl(fd, EVIOCGABS(ABS_MT_POSITION_Y),  &abs_height);
    auto res4 = ioctl(fd, EVIOCGABS(ABS_MT_SLOT),        &abs_slot);

    auto res5 = ioctl(fd, EVIOCGABS(ABS_MT_TOUCH_MAJOR), &abs_major);
    auto res6 = ioctl(fd, EVIOCGABS(ABS_MT_TOUCH_MINOR), &abs_minor);

    if(res1 != 0 || res2 != 0 || res3 != 0 || res4 != 0){
        LOGD("return! %d, %d, %d, %d", res1, res2, res3, res4);
        close(fd);
        return;
    }

    const char* format_str = "%s %d, min %d, max %d";
    LOGD(format_str, "T", abs_tracking_id.value, abs_tracking_id.minimum, abs_tracking_id.maximum);
    LOGD(format_str, "X", abs_width.value,       abs_width.minimum,       abs_width.maximum);
    LOGD(format_str, "Y", abs_height.value,      abs_height.minimum,      abs_height.maximum);
    LOGD(format_str, "S", abs_slot.value,        abs_slot.minimum,        abs_slot.maximum);
    LOGD(format_str, "A", abs_major.value,       abs_major.minimum,       abs_major.maximum);
    LOGD(format_str, "I", abs_minor.value,        abs_minor.minimum,      abs_minor.maximum);

    if(abs_tracking_id.maximum <= 0 || abs_width.maximum <= 0
       || abs_height.maximum <= 0 || abs_slot.maximum <= 0){
        // 大概率不是触摸设备, 或者不是B协议.
        close(fd);
        return;
    }

    char name[80];
    name[sizeof(name) - 1] = '\0';
    if(ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1) {
        LOGD("could not get device name for %s, %s\n", device, strerror(errno));
        name[0] = '\0';
    }
    LOGD("name = %s", name);

    first_dev = (char*)malloc(PATH_MAX);
    first_dev_name = (char*)malloc(PATH_MAX);
    strcpy(first_dev, device);
    strcpy(first_dev_name, name);

    first_dev_width = abs_width.maximum;
    first_dev_height = abs_height.maximum;
    dev_type = 1;
    dev_weight = abs_slot.maximum * 10;
    if(res5 == 0 && abs_major.maximum > 0){
        dev_weight++;
    }
    if(res6 == 0 && abs_minor.maximum > 0){
        dev_weight++;
    }

    close(fd);
}

static int scan_dir(JNIEnv *env, jobject list){
    char devname[PATH_MAX] = INPUT_PATH;

    DIR *dir = opendir(devname);
    if(dir == nullptr){
        return -1;
    }

    auto listClazz = env->FindClass("java/util/AbstractList");
    jmethodID addMethod = env->GetMethodID(listClazz, "add", "(Ljava/lang/Object;)Z");

    auto clazz = env->FindClass("show/taps/DevInfo");
    if(clazz == nullptr){
        return -1;
    }

    auto methodId = env->GetMethodID(clazz, "<init>",
                                 "(Ljava/lang/String;Ljava/lang/String;IIII)V");

    char *filename = devname + strlen(INPUT_PATH);
    struct dirent *de;
    while((de = readdir(dir))) {
        if(de->d_name[0] == '.'){
            // 这里本来是判断的 .\0 和 ..\0, 实际上没必要.
            continue;
        }
        strcpy(filename, de->d_name);
        LOGD("open_device -> %s, %s", filename, devname);
        open_device(devname);

        if(first_dev == nullptr){
            continue;
        }

        auto obj = env->NewObject(clazz,methodId,
                env->NewStringUTF(first_dev), env->NewStringUTF(first_dev_name),
                first_dev_width, first_dev_height, dev_type, dev_weight);

        free(first_dev);
        free(first_dev_name);
        first_dev = nullptr;
        first_dev_name = nullptr;

        env->CallBooleanMethod(list, addMethod, obj);

    }
    closedir(dir);
    return 0;
}

