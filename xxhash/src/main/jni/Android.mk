LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := xxhash
LOCAL_SRC_FILES := xxhash.c
LOCAL_LDLIBS :=  -L$(SYSROOT)/usr/lib
include $(BUILD_SHARED_LIBRARY)
