package com.example.l.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;

import com.example.l.mobilefacenet.util.ImageUtil;

public class CameraPreviewCallback implements PreviewCallback {
    private String TAG;
    private Face mFace;
    private int degrees;;
    private Matrix matrix;
    private int width = 960;
    private int height = 720;
    private CameraActivity cameraActivity;
    private ImageUtil.NV21ToBitmap nv21ToBitmap ;
    private int textSize = 40;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Bitmap bitmap ;
        int _degrees = cameraActivity.getCameraId() == Camera.CameraInfo.CAMERA_FACING_BACK ? degrees : degrees + 90;
        if(_degrees!=0){
            if(matrix==null){
                matrix = new Matrix();
                matrix.setRotate(_degrees, width/2, height/2);
            }
            bitmap = nv21ToBitmap.nv21ToBitmap(data, width, height, matrix);
        }else{
            bitmap = nv21ToBitmap.nv21ToBitmap(data, width, height);
        }

        byte[] imageDate = ImageUtil.getPixelsRGBA(bitmap);
        Face.ColorType colorType = Face.ColorType.R8G8B8A8;

//                    byte[] imageDate = mFace.yuv420sp2Rgb(data, width, height);
//                    bitmap = ImageUtil.rgb2Bitmap(imageDate, width, height);
//                    Face.ColorType colorType = Face.ColorType.R8G8B8;

        long timeDetectFace = System.currentTimeMillis();
        Face.FaceInfo[] faceInfos = mFace.faceDetect(imageDate,width,height,colorType);
        timeDetectFace = System.currentTimeMillis() - timeDetectFace;

        Log.i(TAG, "detect face time:"+timeDetectFace+"ms");
        if(faceInfos !=null && faceInfos.length>0){
//                        Log.i(TAG, "pic width："+width+" height："+height+" face num：" + faceInfos.length );
            Bitmap drawBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            for(int i=0;i<faceInfos.length; i++) {
                Canvas canvas = new Canvas(drawBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.BLUE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                canvas.drawRect(faceInfos[i].getLeft(), faceInfos[i].getTop(), faceInfos[i].getRight(), faceInfos[i].getBottom(), paint);
//                            for(Point p : faceInfos[i].getPoints()){
//                                paint.setColor(Color.RED);
//                                canvas.drawPoint(p.x,p.y,paint);
//                            }

                // 这个方式比较慢
//                            timeDetectFace = System.currentTimeMillis();
//                            float[] feature = mFace.faceFeature(imageDate,width,height,Face.ColorType.R8G8B8A8,faceInfos[i]);
//                            double score = mFace.faceRecognize(feature, cameraActivity.getPersion().getFaceFeature());
//                            timeDetectFace = System.currentTimeMillis() - timeDetectFace;
//                            Log.i(TAG, "recognize face time:"+timeDetectFace+"ms score"+score);

                // 以下方式比上面的要快
                timeDetectFace = System.currentTimeMillis();
                Bitmap faceImage = Bitmap.createBitmap(bitmap, faceInfos[i].getLeft(), faceInfos[i].getTop(), faceInfos[i].getWidth(), faceInfos[i].getHeight());
                byte[] faceDate = ImageUtil.getPixelsRGBA(faceImage);
                float[] feature = mFace.faceFeature(faceDate,faceImage.getWidth(),faceImage.getHeight(),Face.ColorType.R8G8B8A8);
                double score = mFace.faceRecognize(feature, cameraActivity.getPersion().getFaceFeature());
                timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                Log.i(TAG, "recognize face time:"+timeDetectFace+"ms score"+score);

                if(score > Face.THRESHOLD){
                    paint.setColor(Color.GREEN);
                    paint.setTextSize(textSize);
                    paint.setStrokeWidth(3);
                    paint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(cameraActivity.getPersion().getName(),faceInfos[i].getLeft(),faceInfos[i].getTop(),paint);
                }
            }
            cameraActivity.updateImageView(drawBitmap);
        }

//                    if((System.currentTimeMillis() - lastUpdate) > 5 * 1000){
//                        cameraActivity.updateImageView(bitmap);
//                        lastUpdate = System.currentTimeMillis();
//                    }
    }

    public void setTAG(String TAG) {
        this.TAG = TAG;
    }

    public void setmFace(Face mFace) {
        this.mFace = mFace;
    }

    public void setDegrees(int degrees) {
        this.degrees = degrees;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
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
}
