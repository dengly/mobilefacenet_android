
#ifndef FACE_H_
#define FACE_H_

#include "detect.h"
#include "recognize.h"

namespace Face {
    struct FaceEngine {
        // 人脸检测对象指针
        Detect * detect;
        // 人脸比对对象指针
        Recognize * recognize;
    };
}
#endif