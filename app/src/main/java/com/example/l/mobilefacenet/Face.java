package com.example.l.mobilefacenet;

/**
 * Created by L on 2018/6/11.
 */

public class Face {
    private long pFaceEngine;
    private boolean isInit = false;

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @return
     */
    public boolean faceModelInit(String faceDetectionModelPath){
        pFaceEngine = FaceModelInit(faceDetectionModelPath);
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
     * @param imageDate 图片数据 目前只支持BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param imageChannel 图片通道数 rgba是4，rgb是3
     * @return 返回人脸数量、人脸位置坐标、5个关键点
     */
    public int[] faceDetect(byte[] imageDate, int imageWidth , int imageHeight, int imageChannel){
        if(!isInit){
            throw new RuntimeException("人脸识别引擎未初始化");
        }
        return FaceDetect(pFaceEngine, imageDate, imageWidth , imageHeight, imageChannel);
    }

    /**
     * 获取人脸特征码
     * @param imageDate 图片数据 目前只支持RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @return 人脸特征数据
     */
    public float[] faceFeature(byte[] imageDate, int imageWidth , int imageHeight){
        if(!isInit){
            throw new RuntimeException("人脸识别引擎未初始化");
        }
        return FaceFeature(pFaceEngine, imageDate, imageWidth , imageHeight);
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
     * 人脸比对
     * @param faceDate1 图片1数据 目前只支持RGBA
     * @param w1 图片1宽 建议最小是 112
     * @param h1 图片1高 建议最小是 112
     * @param faceDate2 图片2数据 目前只支持RGBA
     * @param w2 图片2宽 建议最小是 112
     * @param h2 图片2高 建议最小是 112
     * @return 返回相识度，[0-1]，0表示100%不像，1表示完全一样
     */
    public double faceRecognize(byte[] faceDate1,int w1,int h1, byte[] faceDate2,int w2,int h2){
        if(!isInit){
            throw new RuntimeException("人脸识别引擎未初始化");
        }
        return FaceRecognize2(pFaceEngine, faceDate1, w1, h1, faceDate2, w2, h2);
    }

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @return
     */
    private native long FaceModelInit(String faceDetectionModelPath);

    /**
     * 反初始化模型
     * @return
     */
    private native boolean FaceModelUnInit(long pFaceEngine);

    /**
     * 人脸检测
     * @param imageDate 图片数据 目前只支持BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param imageChannel 图片通道数 rgba是4，rgb是3
     * @return 返回人脸数量、人脸位置坐标、5个关键点
     */
    private native int[] FaceDetect(long pFaceEngine, byte[] imageDate, int imageWidth , int imageHeight, int imageChannel);

    /**
     * 获取人脸特征码
     * @param imageDate 图片数据 目前只支持RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @return 人脸特征数据
     */
    private native float[] FaceFeature(long pFaceEngine, byte[] imageDate, int imageWidth , int imageHeight);

    /**
     * 人脸特征码对比
     * @param faceFeature1 人脸特征码1数据
     * @param faceFeature2 人脸特征码2数据
     * @return 返回相识度，[0-1]，0表示100%不像，1表示完全一样
     */
    private native double FaceRecognize(float[] faceFeature1, float[] faceFeature2);

    /**
     * 人脸比对
     * @param faceDate1 图片1数据 目前只支持RGBA
     * @param w1 图片1宽 建议最小是 112
     * @param h1 图片1高 建议最小是 112
     * @param faceDate2 图片2数据 目前只支持RGBA
     * @param w2 图片2宽 建议最小是 112
     * @param h2 图片2高 建议最小是 112
     * @return 返回相识度，[0-1]，0表示100%不像，1表示完全一样
     */
    private native double FaceRecognize2(long pFaceEngine, byte[] faceDate1,int w1,int h1,byte[] faceDate2,int w2,int h2);

    static {
        System.loadLibrary("Face");
    }
}
