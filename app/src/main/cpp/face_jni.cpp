#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <cstring>
#include <stdlib.h>

#include "net.h"
#include "mat.h"
#include "face.h"

using namespace Face;
#define TAG "DetectSo"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)

#if !defined(NULL)
#define NULL __null
#endif

extern "C" {

/**
 * 初始化
 */
JNIEXPORT jlong JNICALL
Java_com_example_l_mobilefacenet_Face_FaceModelInit(JNIEnv *env, jobject instance,
                                                    jstring faceDetectionModelPath_, jint threadNum, jint minFaceSize) {
    LOGD("JNI开始人脸检测模型初始化");
    if (NULL == faceDetectionModelPath_) {
        LOGD("导入的人脸检测的目录为空");
        return (jlong)NULL;
    }

    //获取MTCNN模型的绝对路径的目录（不是/aaa/bbb.bin这样的路径，是/aaa/)
    const char *faceDetectionModelPath = env->GetStringUTFChars(faceDetectionModelPath_, 0);
    if (NULL == faceDetectionModelPath) {
        return (jlong)NULL;
    }

    std::string tFaceModelDir = faceDetectionModelPath;
    std::string tLastChar = tFaceModelDir.substr(tFaceModelDir.length() - 1, 1);
    //目录补齐/
    if ("\\" == tLastChar) {
        tFaceModelDir = tFaceModelDir.substr(0, tFaceModelDir.length() - 1) + "/";
    } else if (tLastChar != "/") {
        tFaceModelDir += "/";
    }
    LOGD("init, tFaceModelDir=%s", tFaceModelDir.c_str());

    struct FaceEngine * pFaceEngine = (struct FaceEngine *)malloc(sizeof(struct FaceEngine));

    //没判断是否正确导入，懒得改了
    pFaceEngine->threadNum = threadNum;
    pFaceEngine->minFaceSize = minFaceSize;
    pFaceEngine->detect = new Detect(tFaceModelDir);
    pFaceEngine->recognize = new Recognize(tFaceModelDir);
    pFaceEngine->detect->SetThreadNum(pFaceEngine->threadNum);
    pFaceEngine->recognize->SetThreadNum(pFaceEngine->threadNum);

    env->ReleaseStringUTFChars(faceDetectionModelPath_, faceDetectionModelPath);

    return (jlong)pFaceEngine;
}

/**
 * 反初始化
 */
JNIEXPORT jboolean JNICALL
Java_com_example_l_mobilefacenet_Face_FaceModelUnInit(JNIEnv *env, jobject instance, jlong pFaceEngine_) {
    if (pFaceEngine_ == 0) {
        LOGD("未初始化");
        return true;
    }
    struct FaceEngine * faceEngine = (struct FaceEngine *)pFaceEngine_;

    delete faceEngine->detect;
    delete faceEngine->recognize;

    free(faceEngine);

    LOGD("人脸检测初始化锁，重新置零");
    return true;

}

/**
 * 人脸检测
 */
JNIEXPORT jintArray JNICALL
Java_com_example_l_mobilefacenet_Face_FaceDetect(JNIEnv *env, jobject instance, jlong pFaceEngine_,
                                                 jbyteArray imageDate_, jint imageWidth,
                                                 jint imageHeight, jint colorType) {
    LOGD("JNI开始检测人脸");
    if (pFaceEngine_ == 0) {
        LOGD("未初始化，直接返回空");
        return NULL;
    }
    struct FaceEngine * faceEngine = (struct FaceEngine *)pFaceEngine_;

    if(colorType != ColorType_NV21 && colorType != ColorType_R8G8B8 && colorType != ColorType_B8G8R8 && colorType != ColorType_R8G8B8A8) {
        LOGD("colorType = %x，不支持",colorType);
        return NULL;
    }
    int imageChannel = colorType == ColorType_R8G8B8A8 ? 4 : 3;

    if(colorType != ColorType_NV21){
        int tImageDateLen = env->GetArrayLength(imageDate_);
        if (imageChannel == tImageDateLen / imageWidth / imageHeight) {
            LOGD("数据宽=%d,高=%d,通道=%d", imageWidth, imageHeight, imageChannel);
        } else {
            LOGD("数据长宽高通道不匹配，直接返回空");
            return NULL;
        }
    }

    jbyte *imageDate = env->GetByteArrayElements(imageDate_, NULL);
    if (NULL == imageDate) {
        LOGD("导入数据为空，直接返回空");
        return NULL;
    }

    if (imageWidth < faceEngine->minFaceSize || imageHeight < faceEngine->minFaceSize) {
        LOGD("导入数据的宽和高小于%d，直接返回空", faceEngine->minFaceSize);
        return NULL;
    }

    unsigned char *faceImageCharDate = (unsigned char *) imageDate;

    ncnn::Mat ncnn_img ;
    unsigned char * pRgb;
    if(colorType != ColorType_NV21){
        int pixel_type = colorType == ColorType_B8G8R8 ? ncnn::Mat::PIXEL_BGR2RGB : colorType == ColorType_R8G8B8A8 ? ncnn::Mat::PIXEL_RGBA2RGB : ncnn::Mat::PIXEL_RGB ;
        ncnn_img = ncnn::Mat::from_pixels(faceImageCharDate, pixel_type, imageWidth, imageHeight) ;
    }else{
        int rgb_len = 3 * imageWidth * imageHeight;
        pRgb = (unsigned char *)malloc(sizeof(unsigned char) * rgb_len);
        ncnn::yuv420sp2rgb(faceImageCharDate, imageWidth, imageHeight, pRgb);
        ncnn_img = ncnn::Mat::from_pixels(pRgb, ncnn::Mat::PIXEL_RGB, imageWidth, imageHeight) ;
    }

    LOGD("JNI开始检测人脸");
    int32_t minFaceSize = faceEngine->minFaceSize;
    faceEngine->detect->SetMinFace(minFaceSize);

    std::vector<Bbox> finalBbox;
    // 开始人脸检测
    faceEngine->detect->start(ncnn_img, finalBbox);

    int32_t num_face = static_cast<int32_t>(finalBbox.size());
    LOGD("检测到的人脸数目：%d\n", num_face);

    int out_size = 1 + num_face * 14;
    //  LOGD("内部人脸检测完成,开始导出数据");
    int *faceInfo = new int[out_size];
    faceInfo[0] = num_face;
    for (int i = 0; i < num_face; i++) {
        faceInfo[14 * i + 1] = finalBbox[i].x1;//left
        faceInfo[14 * i + 2] = finalBbox[i].y1;//top
        faceInfo[14 * i + 3] = finalBbox[i].x2;//right
        faceInfo[14 * i + 4] = finalBbox[i].y2;//bottom
        for (int j = 0; j < 10; j++) { // 五个关键点 x1,x2,x3,x4,x5,y1,y2,y3,y4,y5
            faceInfo[14 * i + 5 + j] = static_cast<int>(finalBbox[i].ppoint[j]);
        }
    }

    jintArray tFaceInfo = env->NewIntArray(out_size);
    env->SetIntArrayRegion(tFaceInfo, 0, out_size, faceInfo);
    //  LOGD("内部人脸检测完成,导出数据成功");
    delete[] faceInfo;

    env->ReleaseByteArrayElements(imageDate_, imageDate, 0);
    if(colorType == ColorType_NV21){
        free(pRgb);
    }
    return tFaceInfo;
}

/**
 * 获取人脸特征码
 */
JNIEXPORT jfloatArray JNICALL
Java_com_example_l_mobilefacenet_Face_FaceFeature(JNIEnv *env, jobject instance, jlong pFaceEngine_,
                                                  jbyteArray faceDate_, jint w1, jint h1, jint colorType) {
    if (pFaceEngine_ == 0) {
        LOGD("未初始化，直接返回空");
        return NULL;
    }
    if(colorType != ColorType_NV21 && colorType != ColorType_R8G8B8 && colorType != ColorType_B8G8R8 && colorType != ColorType_R8G8B8A8) {
        LOGD("colorType = %x，不支持",colorType);
        return NULL;
    }
    struct FaceEngine * faceEngine = (struct FaceEngine *)pFaceEngine_;

    jbyte *faceDate = env->GetByteArrayElements(faceDate_, NULL);

    unsigned char *faceImageCharDate = (unsigned char*)faceDate;

    ncnn::Mat ncnn_img;
    unsigned char * pRgb;
    if(colorType != ColorType_NV21){
        int pixel_type = colorType == ColorType_B8G8R8 ? ncnn::Mat::PIXEL_BGR2RGB : colorType == ColorType_R8G8B8A8 ? ncnn::Mat::PIXEL_RGBA2RGB : ncnn::Mat::PIXEL_RGB ;
        // 没进行对齐操作，且以下对图像缩放的操作方法对结果影响较大。可改进空间很大，有能力的自己改改
        ncnn_img = ncnn::Mat::from_pixels_resize(faceImageCharDate, pixel_type, w1, h1, 112, 112);
    }else{
        int rgb_len = 3 * w1 * h1;
        pRgb = (unsigned char *)malloc(sizeof(unsigned char) * rgb_len);
        ncnn::yuv420sp2rgb(faceImageCharDate, w1, h1, pRgb);
        ncnn_img = ncnn::Mat::from_pixels_resize(pRgb, ncnn::Mat::PIXEL_RGB, w1, h1, 112, 112);
    };

    std::vector<float> feature;
    // 获取人脸特征
    faceEngine->recognize->start(ncnn_img, feature);

    env->ReleaseByteArrayElements(faceDate_, faceDate, 0);

    int feature_size = feature.size();

    float *faceFeature = new float[feature_size];
    if (!feature.empty()) {
        memcpy(faceFeature, &feature[0], feature.size()*sizeof(float));
    }

    jfloatArray tFaceFeature = env->NewFloatArray(feature_size);
    env->SetFloatArrayRegion(tFaceFeature, 0, feature_size, faceFeature);
    delete[] faceFeature;

    env->ReleaseByteArrayElements(faceDate_, faceDate, 0);
    if(colorType == ColorType_NV21){
        free(pRgb);
    }
    return tFaceFeature;
}

/**
 * 人脸特征码对比
 */
JNIEXPORT jdouble JNICALL
Java_com_example_l_mobilefacenet_Face_FaceRecognize(JNIEnv *env, jobject instance,
                                                    jfloatArray faceFeature1_, jfloatArray faceFeature2_) {

    jfloat *faceFeature1 = env->GetFloatArrayElements(faceFeature1_, NULL);
    jfloat *faceFeature2 = env->GetFloatArrayElements(faceFeature2_, NULL);

    int len_feature1 = env->GetArrayLength(faceFeature1_);
    int len_feature2 = env->GetArrayLength(faceFeature2_);

    float * p_feature1 = (float *)faceFeature1;
    float * p_feature2 = (float *)faceFeature2;

    //通过数组a的地址初始化，注意地址是从0到5（左闭右开区间）
    std::vector<float> feature1(p_feature1, p_feature1 + len_feature1);
    std::vector<float> feature2(p_feature2, p_feature2 + len_feature2);

    env->ReleaseFloatArrayElements(faceFeature1_, faceFeature1, 0);
    env->ReleaseFloatArrayElements(faceFeature2_, faceFeature2, 0);
    // 人脸特征比对
    double similar = calculSimilar(feature1, feature2);
    return similar;
}

/**
 * nv21转rgb
 */
JNIEXPORT jbyteArray JNICALL
Java_com_example_l_mobilefacenet_Face_Yuv420sp2Rgb(JNIEnv *env, jclass clazz,
                                                   jbyteArray _yuv420sp, jint _w, jint _h) {
    jbyte * p_yuv420sp = env->GetByteArrayElements(_yuv420sp, NULL);
    unsigned char * pYuv420sp = (unsigned char*)p_yuv420sp;
    int rgb_len = 3 * _w *_h;
    unsigned char * pRgb = (unsigned char *)malloc(sizeof(unsigned char) * rgb_len);
    ncnn::yuv420sp2rgb(pYuv420sp, _w, _h, pRgb);

    jbyteArray rgb = env->NewByteArray(rgb_len);
    env->SetByteArrayRegion(rgb, 0, rgb_len, (jbyte *)pRgb);
    env->ReleaseByteArrayElements(_yuv420sp, p_yuv420sp, 0);
    free(pRgb);
    return rgb;
}

/**
 * nv21 裁剪
 * 参考 https://blog.csdn.net/Magic_frank/article/details/70941111
 */
JNIEXPORT jbyteArray JNICALL
Java_com_example_l_mobilefacenet_Face_CutNV21(JNIEnv *env, jclass clazz, jbyteArray _yuv420sp, jint x,
            jint y, jint cutW, jint cutH, jint srcW, jint srcH) {
    jbyte * p_yuv420sp = env->GetByteArrayElements(_yuv420sp, NULL);
    unsigned char *srcNv21 = (unsigned char*)p_yuv420sp;

    int i;
    int j = 0;
    int k = 0;

    int len = cutW * cutH * 3 / 2 ;
    unsigned char *tarNv21 = (unsigned char *)malloc(sizeof(unsigned char) * len);
    //分配一段内存，用于存储裁剪后的Y分量
    unsigned char *tmpY = (unsigned char *)malloc(sizeof(unsigned char) * cutW * cutH);
    //分配一段内存，用于存储裁剪后的UV分量
    unsigned char *tmpUV = (unsigned char *)malloc(sizeof(unsigned char) * cutW * cutH / 2);

    for(i=y; i< cutH + y; i++) {
        // 逐行拷贝Y分量，共拷贝cutW*cutH
        memcpy(tmpY + j * cutW, srcNv21 + x + i * srcW, cutW);
        j++;
    }
    for(i=y/2; i< (cutH + y) / 2; i++) {
        //逐行拷贝UV分量，共拷贝cutW*cutH/2
        memcpy(tmpUV + k * cutW, srcNv21 + x + srcW * srcH + i * srcW, cutW);
        k++;
    }
    //将拷贝好的Y，UV分量拷贝到目标内存中
    memcpy(tarNv21, tmpY, cutW * cutH);
    memcpy(tarNv21 + cutW * cutH, tmpUV, cutW * cutH / 2);

    jbyteArray rev = env->NewByteArray(len);
    env->SetByteArrayRegion(rev, 0, len, (jbyte *)tarNv21);
    env->ReleaseByteArrayElements(_yuv420sp, p_yuv420sp, 0);
    free(tarNv21);
    free(tmpUV);
    free(tmpY);
    return rev;
}

}
