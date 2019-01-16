package com.example.l.mobilefacenet;

/**
 * Created by L on 2018/6/11.
 */

public class Face {

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @return
     */
    public native boolean FaceModelInit(String faceDetectionModelPath);

    /**
     * 人脸检测
     * @param imageDate 图片数据 目前只支持BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param imageChannel 图片通道数 rgba是4，rgb是3
     * @return 返回人脸数量、人脸位置坐标、5个关键点
     */
    public native int[] FaceDetect(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);

    /**
     * 反初始化模型
     * @return
     */
    public native boolean FaceModelUnInit();

    /**
     * 人脸比对
     * @param faceDate1 图片1数据 目前只支持RGBA
     * @param w1 图片1宽 建议最小是 112
     * @param h1 图片1高 建议最小是 112
     * @param faceDate2 图片2数据 目前只支持RGBA
     * @param w2 图片2宽 建议最小是 112
     * @param h2 图片2高 建议最小是 112
     * @return 返回相识度
     */
    public native double FaceRecognize(byte[] faceDate1,int w1,int h1,byte[] faceDate2,int w2,int h2);

    static {
        System.loadLibrary("Face");

    }
}
