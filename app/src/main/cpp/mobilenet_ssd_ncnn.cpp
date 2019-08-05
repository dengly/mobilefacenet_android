//
// Created by 邓燎燕 on 2019/2/15.
//

#include "mobilenet_ssd_ncnn.h"

namespace Face {

    MobilenetSSDDetection::MobilenetSSDDetection(const std::string &model_path) {
//        std::string param_files = model_path + "/mobilenet_ssd_ncnn.proto";
//        std::string bin_files = model_path + "/mobilenet_ssd_ncnn.bin";
        std::string param_files = model_path + "mobilenet_ssd_ncnn2.param";
        std::string bin_files = model_path + "mobilenet_ssd_ncnn2.bin";
        SSDDetectionNet.load_param(param_files.c_str());
        SSDDetectionNet.load_model(bin_files.c_str());

#if NCNN_VULKAN
        SSDDetectionNet.opt.use_vulkan_compute = 1;
#endif // NCNN_VULKAN
    }

    MobilenetSSDDetection::~MobilenetSSDDetection() {
        SSDDetectionNet.clear();
    }

    int MobilenetSSDDetection::detectFace(const ncnn::Mat &img, std::vector<Bbox> &boxes){
//        const float mean_vals[3] = { 104.0f, 117.0f, 123.0f };
        const float mean_vals[3] = {127.5f, 127.5f, 127.5f};
        const float norm_vals[3] = {1.0/127.5,1.0/127.5,1.0/127.5};

        const int cols = img.w;
        const int rows = img.h;
//        Mat in = ncnn::Mat::from_pixels_resize((unsigned char*)img.data, ncnn::Mat::PIXEL_BGR, cols, rows, 224, 224);
        Mat in = img;

//        in.substract_mean_normalize(mean_vals, 0);
        in.substract_mean_normalize(mean_vals, norm_vals);

        Mat out;
        Extractor ex = SSDDetectionNet.create_extractor();
        ex.set_light_mode(true);
        ex.set_num_threads(threadnum);
        ex.input("data", in);
        ex.extract("detection_out", out);
        std::vector<std::vector<float>> detections;
        for (int ih = 0; ih < out.h; ih++) {
            vector<float> detection;
            for (int iw = 0; iw < out.w; iw++)  {
                detection.push_back(out[iw + ih*out.w]);
            }
            detections.push_back(detection);
        }
        if(detections.size() == 0){
            return 0;
        }
        for(vector<vector<float>>::iterator bbox = detections.begin(); bbox!= detections.end(); bbox++){
            vector<float> b = *bbox;
            if (b[1] < 0.7) {
                continue;
            }
            int x = static_cast<int>(b[2] * cols);
            int y = static_cast<int>(b[3] * rows);
            int w = static_cast<int>(b[4] * cols - x);
            int h = static_cast<int>(b[5] * rows - y);
            Bbox box;
            box.score = b[1];
            box.x1 = x;
            box.y1 = y;
            box.x2 = x+w;
            box.y2 = y+h;
            box.area = w * h;
            boxes.push_back(box);
        }

        return 0;
    }

    void MobilenetSSDDetection::SetThreadNum(int threadNum) {
        threadnum = threadNum;
    }

}