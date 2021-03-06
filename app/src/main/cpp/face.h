
#ifndef FACE_H_
#define FACE_H_

#include "detect.h"
#include "recognize.h"
#include "mobilenet_ssd_ncnn.h"


#define ColorType_R8G8B8 0x101
#define ColorType_B8G8R8 0x102
#define ColorType_R8G8B8A8 0x103
#define ColorType_NV21 0x104

namespace Face {

    typedef struct FaceEngine{
        int threadNum;
        int32_t minFaceSize;
        // 人脸检测对象指针
        Detect * detect;
        // 人脸比对对象指针
        Recognize * recognize;
        // 人脸检测对象指针
        MobilenetSSDDetection * ssdDetection;
    } FaceEngine;
}
#endif