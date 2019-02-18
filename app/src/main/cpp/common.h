//
// Created by 邓燎燕 on 2019/2/15.
//

#ifndef MOBILEFACENET_ANDROID_COMMON_H
#define MOBILEFACENET_ANDROID_COMMON_H

typedef struct Bbox {
    float score;
    int x1; //left
    int y1; //top
    int x2; //right
    int y2; //bottom
    float area;
    float ppoint[10]; // 五个关键点 x1,x2,x3,x4,x5,y1,y2,y3,y4,y5
    float regreCoord[4];
};

#endif //MOBILEFACENET_ANDROID_COMMON_H
