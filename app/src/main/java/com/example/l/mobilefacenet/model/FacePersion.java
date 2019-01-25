package com.example.l.mobilefacenet.model;

import com.example.l.mobilefacenet.Face;

public class FacePersion {
    // 人脸跟踪ID
    public Long trackId;
    // 人脸检查结果 类型根据实际情况修改
    public Face.FaceInfo faceInfo;
    // 人脸检查结果对应的人脸图片数据 类型根据实际情况修改
    public byte[] faceDate;
    // 人脸特征码 类型根据实际情况修改
    public float[] feature;
    // 人脸识别结果
    public String name;
    // 人脸识别成绩
    public double score;

    public long time;
}
