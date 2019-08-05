package com.example.l.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import com.example.l.mobilefacenet.model.Persion;
import com.example.l.mobilefacenet.util.CommonUtil;
import com.example.l.mobilefacenet.util.ImageUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CameraPreviewCallback implements AbstractCameraPreviewCallback {
    private final static String TAG = LiveCameraView.class.getSimpleName();
    private Face mFace = new Face();
    private int degrees;
    private Matrix matrix;
    private int width = 640;
    private int height = 480;
    private CameraActivity cameraActivity;
    private ImageUtil.NV21ToBitmap nv21ToBitmap ;
    private int textSize = 40;

    private List<Persion> persions;

    public CameraPreviewCallback(){
        //model init
        File sdDir = Environment.getExternalStorageDirectory();//get directory
        String sdPath = sdDir.toString() + "/facem/";
//        mFace.faceModelInit(sdPath, AndroidUtil.getNumberOfCPUCores()*2, Face.MIN_FACE_SIZE);
//        int minFaceSize = CommonUtil.max(width, height) / Face.MIN_FACE_SIZE_SIDE_SCALE;
        int minFaceSize = CommonUtil.sqrt(CommonUtil.area(width, height) / Face.MIN_FACE_SIZE_SCALE);
        int threadNum = 4;
        mFace.faceModelInit(sdPath, threadNum, minFaceSize);
    }

    public void stop(){
        if(mFace!=null){
            mFace.faceModelUnInit();
            mFace = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
        final byte[] frame = Arrays.copyOf(data,data.length);
        long timeDetectFace = System.currentTimeMillis();
        Face.FaceInfo[] faceInfos = mFace.faceDetect(Face.DETECTTYPE_MTCNN, frame, width, height, Face.ColorType.NV21);
        timeDetectFace = System.currentTimeMillis() - timeDetectFace;

        Log.i(TAG, "detect face time:"+timeDetectFace+"ms");
        if(faceInfos !=null && faceInfos.length>0){
            Bitmap drawBitmap = nv21ToBitmap.nv21ToBitmap(frame, width, height);
            for(int i=0;i<faceInfos.length; i++) {
                Face.FaceInfo faceInfo = faceInfos[i];

                Canvas canvas = new Canvas(drawBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.BLUE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                canvas.drawRect(faceInfo.getLeft(), faceInfo.getTop(), faceInfo.getRight(), faceInfo.getBottom(), paint);

                int tempW,tempH;
                if((faceInfo.getMaxSide()+faceInfo.getLeft()) > width
                        || (faceInfo.getMaxSide()+faceInfo.getTop()) > height){
                    tempW = faceInfo.getWidth();
                    tempH = faceInfo.getHeight();
                }else{
                    tempW = faceInfo.getMaxSide();
                    tempH = faceInfo.getMaxSide();
                }
                tempW = tempW % 2 ==0 ? tempW : tempW -1;
                tempH = tempH % 2 ==0 ? tempH : tempH -1;
                //byte[] faceDate = Face.cutNV21(videoFrame.frame, faceInfo.getLeft(), faceInfo.getTop(), tempW, tempH, width, height);
                byte[] faceDate = ImageUtil.cutNV21(frame, faceInfo.getLeft(), faceInfo.getTop(), tempW, tempH, width, height);
                timeDetectFace = System.currentTimeMillis();
                float[] feature = mFace.faceFeature(faceDate, tempW, tempH, Face.ColorType.NV21);
                Log.i(TAG, "face feature time:"+(System.currentTimeMillis() - timeDetectFace)+"ms");
                double maxScore=0;
                int index=-1;
                for(int j =0; j<persions.size(); j++){
                    Persion persion = persions.get(j);
                    timeDetectFace = System.currentTimeMillis();
                    double score = mFace.faceRecognize(feature, persion.getFaceFeature());
                    Log.i(TAG, "recognize face time:"+(System.currentTimeMillis() - timeDetectFace)+"ms score:"+maxScore);
                    if(score > maxScore){
                        index = j;
                        maxScore = score;
                    }
                }

                paint.setColor(Color.GREEN);
                paint.setTextSize(textSize);
                paint.setStrokeWidth(3);
                paint.setTextAlign(Paint.Align.LEFT);
                if(maxScore > Face.THRESHOLD){
                    canvas.drawText(String.format("%s-%.3f", persions.get(index).getName(), maxScore),faceInfo.getLeft(),faceInfo.getTop(),paint);
                }else{
                    canvas.drawText(String.format("未知-%.3f", maxScore),faceInfo.getLeft(),faceInfo.getTop(),paint);
                }
            }
            cameraActivity.updateImageView(drawBitmap);
        }
    }

    public void setDegrees(int degrees) {
        this.degrees = degrees;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCameraActivity(CameraActivity cameraActivity) {
        this.cameraActivity = cameraActivity;
    }

    public void setNv21ToBitmap(ImageUtil.NV21ToBitmap nv21ToBitmap) {
        this.nv21ToBitmap = nv21ToBitmap;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setPersions(List<Persion> persions) {
        this.persions = persions;
    }
}
