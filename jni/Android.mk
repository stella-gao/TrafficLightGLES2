LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := TrafficLightGLES2
LOCAL_SRC_FILES := TrafficLightGLES2.cpp

include $(BUILD_SHARED_LIBRARY)
