/*
 * Copyright 2021, Digi International Inc.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#define LOG_TAG "WifiChipUtilsNative"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include "core_jni_helpers.h"

#include "cutils/misc.h"

#include <log/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#define QCA_MODULE_PATH   "/vendor/lib/modules/qca6564_wlan.ko"
#define QCA_MODULE_ALIAS  "wlan"
#define QCA_MODULE_ARGS   "asyncintdelay=0x2 writecccr1=0xf2 writecccr1value=0xf writecccr2=0xf1 writecccr2value=0xa8 writecccr3=0xf0  writecccr3value=0xa1 writecccr4=0x15 writecccr4value=0x30 enable_p2p=1"

static const char MODULE_PATH_QUALCOMM[] = QCA_MODULE_PATH;
static const char MODULE_ARGS_QUALCOMM[] = QCA_MODULE_ARGS;
static const char MODULE_TAG_QUALCOMM[]  = QCA_MODULE_ALIAS " ";
static const char MODULE_FILE[]          = "/proc/modules";

#ifdef __UCLIBC__
extern int init_module(void *module, unsigned long len, const char *options);
extern int delete_module(const char *module, unsigned int flags);
#else
# include <sys/syscall.h>
# define init_module(mod, len, opts) syscall(__NR_init_module, mod, len, opts)
# define delete_module(mod, flags) syscall(__NR_delete_module, mod, flags)
#endif

namespace android {

int is_module_loaded(const char *module_name) {
    char line[sizeof(module_name) + 10];
    FILE *proc;

    if ((proc = fopen(MODULE_FILE, "r")) == NULL) {
        ALOGW("Could not open %s: %s", MODULE_FILE, strerror(errno));
        return 0;
    }
    while ((fgets(line, sizeof(line), proc)) != NULL) {
        if (strncmp(line, module_name, strlen(module_name)) == 0) {
            fclose(proc);
            return 1;
        }
    }
    fclose(proc);

    return 0;
}

static int insmod(const char *filename, const char *args) {
    void *module;
    unsigned int size;
    int ret;

    module = load_file(filename, &size);
    if (!module) {
        ALOGE("Could not load module file \"%s\"", filename);
        return -1;
    }

    ret = init_module(module, size, args);

    free(module);

    return ret;
}

static int rmmod(const char *modname) {
    int ret = -1;
    int maxtry = 10;

    while (maxtry-- > 0) {
        ret = delete_module(modname, O_NONBLOCK | O_EXCL);
        if (ret < 0 && errno == EAGAIN)
            usleep(500000);
        else
            break;
    }

    if (ret != 0)
        ALOGD("Unable to unload driver module \"%s\": %s\n",
                modname, strerror(errno));
    return ret;
}

static void loadQCAModule(JNIEnv* env, jobject clazz) {
    if (is_module_loaded(MODULE_TAG_QUALCOMM))
        return;

    if (insmod(MODULE_PATH_QUALCOMM, MODULE_ARGS_QUALCOMM) != 0) {
        ALOGE("Error loading QCA module");
    }
}

static void unloadQCAModule(JNIEnv* env, jobject clazz) {
    if (!is_module_loaded(MODULE_TAG_QUALCOMM))
        return;

    if (rmmod(QCA_MODULE_ALIAS) != 0) {
        ALOGE("Error unloading QCA module");
    }
}

// ----------------------------------------------------------------------------
// JNI Glue
// ----------------------------------------------------------------------------

const char* const kClassPathName = "com/android/internal/util/WifiChipUtils";

static const JNINativeMethod gMethods[] = {
    { "nativeLoadQCAModule", "()V", (void*) loadQCAModule },
    { "nativeUnloadQCAModule", "()V", (void*) unloadQCAModule },
};

int register_com_android_internal_util_WifiChipUtils(JNIEnv* env) {
    return RegisterMethodsOrDie(env, kClassPathName, gMethods, NELEM(gMethods));
}


} // namespace android
