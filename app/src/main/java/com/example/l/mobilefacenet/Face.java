package com.example.l.mobilefacenet;

/**
 * Created by L on 2018/6/11.
 */

public class Face {
    public enum ColorType {
        R8G8B8(0x101),
        B8G8R8(0x102),
        R8G8B8A8(0x103);

        private int code;
        private ColorType(int code) {
            this.code = code;
        }
        public int getCode(){
            return code;
        }
    }

    private long pFaceEngine;
    private boolean isInit = false;

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @return
     */
    public boolean faceModelInit(String faceDetectionModelPath){
        pFaceEngine = FaceModelInit(faceDetectionModelPath, 2, 80);
        isInit = pFaceEngine!=0;
        return isInit;
    }

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @param threadNum 线程数
     * @param minFaceSize 最小人脸像素
     * @return
     */
    public boolean faceModelInit(String faceDetectionModelPath, int threadNum, int minFaceSize){
        pFaceEngine = FaceModelInit(faceDetectionModelPath, threadNum, minFaceSize);
        isInit = pFaceEngine!=0;
        return isInit;
    }

    /**
     * 反初始化模型
     * @return
     */
    public boolean faceModelUnInit(){
        if(isInit){
            return FaceModelUnInit(pFaceEngine);
        }
        return true;
    }

    /**
     * 人脸检测
     * @param imageDate 图片数据 目前只支持RGB、BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param colorType 图片颜色类型
     * @return 返回人脸数量、人脸位置坐标、5个关键点
     */
    public int[] faceDetect(byte[] imageDate, int imageWidth , int imageHeight, ColorType colorType){
        if(!isInit){
            throw new RuntimeException("人脸识别引擎未初始化");
        }
        return FaceDetect(pFaceEngine, imageDate, imageWidth , imageHeight, colorType.code);
    }

    /**
     * 获取人脸特征码
     * @param imageDate 图片数据 目前只支持RGB、BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param colorType 图片颜色类型
     * @return 人脸特征数据
     */
    public float[] faceFeature(byte[] imageDate, int imageWidth , int imageHeight, ColorType colorType){
        if(!isInit){
            throw new RuntimeException("人脸识别引擎未初始化");
        }
        return FaceFeature(pFaceEngine, imageDate, imageWidth , imageHeight, colorType.code);
    }

    /**
     * 人脸特征码对比
     * @param faceFeature1 人脸特征码1数据
     * @param faceFeature2 人脸特征码2数据
     * @return 返回相识度，[0-1]，0表示100%不像，1表示完全一样
     */
    public double faceRecognize(float[] faceFeature1, float[] faceFeature2){
        return FaceRecognize(faceFeature1, faceFeature2);
    }

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @param threadNum 线程数
     * @param minFaceSize 最小人脸像素
     * @return
     */
    private native long FaceModelInit(String faceDetectionModelPath, int threadNum, int minFaceSize);

    /**
     * 反初始化模型
     * @return
     */
    private native boolean FaceModelUnInit(long pFaceEngine);

    /**
     * 人脸检测
     * @param imageDate 图片数据 目前只支持RGB、BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param colorType 图片颜色类型
     * @return 返回人脸数量、人脸位置坐标、5个关键点
     */
    private native int[] FaceDetect(long pFaceEngine, byte[] imageDate, int imageWidth , int imageHeight, int colorType);

    /**
     * 获取人脸特征码
     * @param imageDate 图片数据 目前只支持RGB、BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param colorType 图片颜色类型
     * @return 人脸特征数据
     */
    private native float[] FaceFeature(long pFaceEngine, byte[] imageDate, int imageWidth , int imageHeight, int colorType);

    /**
     * 人脸特征码对比
     * @param faceFeature1 人脸特征码1数据
     * @param faceFeature2 人脸特征码2数据
     * @return 返回相识度，[0-1]，0表示100%不像，1表示完全一样
     */
    private native double FaceRecognize(float[] faceFeature1, float[] faceFeature2);

    static {
        System.loadLibrary("Face");
    }
}
