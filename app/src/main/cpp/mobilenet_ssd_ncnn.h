//
// Created by 邓燎燕 on 2019/2/15.
//
#pragma once
#ifndef MOBILEFACENET_ANDROID_MOBILENET_SSD_NCNN_H
#define MOBILEFACENET_ANDROID_MOBILENET_SSD_NCNN_H

#include <string>
#include <algorithm>

#include "net.h"
#include "common.h"

using namespace std;
using namespace ncnn;

namespace Face {
    class MobilenetSSDDetection {
    public:
        MobilenetSSDDetection(const std::string &model_path);
        ~MobilenetSSDDetection();
        int detectFace(const ncnn::Mat &img, std::vector<Bbox> &boxes);

        void SetThreadNum(int threadNum);

    private:
        ncnn::Net SSDDetectionNet;
        int threadnum;
    };
}


#endif //MOBILEFACENET_ANDROID_MOBILENET_SSD_NCNN_H
