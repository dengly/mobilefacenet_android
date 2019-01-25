package com.example.l.mobilefacenet.model;

import android.graphics.Bitmap;

import com.example.l.mobilefacenet.Face;

import java.util.Map;

public class VideoFrame {
    // 帧生成时间戳
    public long time;
    // 帧数据 类型根据实际情况修改
    public byte[] frame;
    public Bitmap bitmap;
    // 帧数据 宽
    public int width;
    // 帧数据 高
    public int height;
    // 相关人脸
    public Map<Long,Face.FaceInfo> trackMap;
}
