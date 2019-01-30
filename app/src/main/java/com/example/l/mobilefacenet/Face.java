package com.example.l.mobilefacenet;

import android.graphics.Point;

/**
 * Created by L on 2018/6/11.
 */

public class Face {
    public enum ColorType {
        R8G8B8(0x101),
        B8G8R8(0x102),
        R8G8B8A8(0x103),
        NV21(0x104);

        private int code;
        private ColorType(int code) {
            this.code = code;
        }
        public int getCode(){
            return code;
        }
    }
    public class FaceInfo {
        private int left, top, right, bottom;
        private Point[] points;

        public int getMaxSide(){
            int w = right - left;
            int h = bottom - top;
            return w>h?w:h;
        }

        public int getWidth() {
            return right - left;
        }

        public int getHeight() {
            return bottom - top;
        }

        public int getLeft() {
            return left;
        }

        public int getTop() {
            return top;
        }

        public int getRight() {
            return right;
        }

        public int getBottom() {
            return bottom;
        }

        public Point[] getPoints() {
            return points;
        }
    }

    private long pFaceEngine;
    private boolean isInit = false;

    public static final int MIN_FACE_SIZE = 112;
    public static final int MIN_FACE_SIZE_SCALE = 24;
    public static final int MIN_FACE_SIZE_SIDE_SCALE = 6;
    public static final double THRESHOLD = 0.6;

    /**
     * nv21转rgb
     * @param yuv420sp
     * @param w
     * @param h
     * @return
     */
    public static byte[] yuv420sp2Rgb(byte[] yuv420sp, int w, int h){
        return Yuv420sp2Rgb(yuv420sp, w, h);
    }

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @return
     */
    public boolean faceModelInit(String faceDetectionModelPath){
        return faceModelInit(faceDetectionModelPath, 2, MIN_FACE_SIZE);
    }

    /**
     * 初始化模型
     * @param faceDetectionModelPath
     * @param threadNum 线程数 注意设置大了不一定快，建议根据时间设备情况调整
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
     * @param imageDate 图片数据 目前只支持nv21、RGB、BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param colorType 图片颜色类型
     * @return 返回人脸数量、人脸位置坐标、5个关键点
     */
    public FaceInfo[] faceDetect(byte[] imageDate, int imageWidth , int imageHeight, ColorType colorType){
        if(!isInit){
            throw new RuntimeException("人脸识别引擎未初始化");
        }
        int[] faceInfo = FaceDetect(pFaceEngine, imageDate, imageWidth , imageHeight, colorType.code);
        if(faceInfo==null || faceInfo[0] <= 0){
            return null;
        }
        FaceInfo[] faceInfos = new FaceInfo[faceInfo[0]];
        for(int i=0;i<faceInfo[0];i++) {
            FaceInfo item = new FaceInfo();
            item.left = faceInfo[1+14*i];
            item.top = faceInfo[2+14*i];
            item.right = faceInfo[3+14*i];
            item.bottom = faceInfo[4+14*i];
            Point[] points = new Point[5];
            for(int j=0; j<5; j++){
                Point point = new Point(faceInfo[5+14*i+j],faceInfo[10+14*i+j]);
                points[j] = point;
            }
            item.points = points;
            faceInfos[i] = item;
        }
        return faceInfos;
    }

    /**
     * 获取人脸特征码
     * @param imageDate 图片数据 目前只支持nv21、RGB、BGR和RGBA
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
     * 获取人脸特征码
     * @param imageDate 图片数据 目前只支持nv21、RGB、BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param colorType 图片颜色类型
     * @param faceInfo 人脸信息，人脸位置等
     * @return 人脸特征数据
     */
    public float[] faceFeature(byte[] imageDate, int imageWidth , int imageHeight, ColorType colorType, FaceInfo faceInfo){
        if(!isInit){
            throw new RuntimeException("人脸识别引擎未初始化");
        }
        if(faceInfo == null){
            throw new RuntimeException("没有人脸位置信息");
        }
        byte[] faceImageDate = getFaceImage(imageDate, imageWidth , imageHeight, colorType, faceInfo);
        return FaceFeature(pFaceEngine, faceImageDate, faceInfo.getWidth() , faceInfo.getHeight(), colorType.code);
    }

    public byte[] getFaceImage(byte[] imageDate, int imageWidth , int imageHeight, ColorType colorType, FaceInfo faceInfo){
        if(faceInfo == null){
            throw new RuntimeException("没有人脸位置信息");
        }
        int channel = colorType == ColorType.R8G8B8A8 ? 4 : 3;
        byte[] faceImageDate = new byte[channel * faceInfo.getWidth() * faceInfo.getHeight()];
        int wlen = 0;
        int hlen = 0;
        for(int i =0 ; i< faceImageDate.length;){
            if(wlen >= faceInfo.getWidth()){
                wlen = 0;
                hlen++;
            }
            int index = wlen + faceInfo.left + (faceInfo.top + hlen) * imageWidth;
            index = channel * index;
            faceImageDate[i+0] = imageDate[index+0];
            faceImageDate[i+1] = imageDate[index+1];
            faceImageDate[i+2] = imageDate[index+2];
            if(channel == 4) {
                faceImageDate[i+3] = imageDate[index+3];
                i = i+4 ;
            }else{
                i = i+3 ;
            }
            wlen++;
        }
        return faceImageDate;
    }

    public static byte[] cutNV21(byte[] nv21, int x, int y, int cutW, int cutH, int srcW, int srcH){
        if(nv21 == null || nv21.length==0){
            return null;
        }
        return CutNV21(nv21, x, y, cutW, cutH, srcW, srcH);
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
     * @param imageDate 图片数据 目前只支持nv21、RGB、BGR和RGBA
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param colorType 图片颜色类型
     * @return 返回人脸数量、人脸位置坐标、5个关键点
     */
    private native int[] FaceDetect(long pFaceEngine, byte[] imageDate, int imageWidth , int imageHeight, int colorType);

    /**
     * 获取人脸特征码
     * @param imageDate 图片数据 目前只支持nv21、RGB、BGR和RGBA
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

    /**
     * nv21转rgb
     * @param yuv420sp
     * @param w
     * @param h
     * @return
     */
    private static native byte[] Yuv420sp2Rgb(byte[] yuv420sp, int w, int h);

    private static native byte[] CutNV21(byte[] nv21, int x, int y, int cutW, int cutH, int srcW, int srcH);

    static {
        System.loadLibrary("Face");
    }
}
