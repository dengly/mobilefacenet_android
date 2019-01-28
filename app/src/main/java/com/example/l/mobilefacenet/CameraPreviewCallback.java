package com.example.l.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import com.example.l.mobilefacenet.model.Persion;
import com.example.l.mobilefacenet.util.CommonUtil;
import com.example.l.mobilefacenet.util.ImageUtil;

import java.io.File;
import java.util.List;

public class CameraPreviewCallback implements AbstractCameraPreviewCallback {
    private final static String TAG = LiveCameraView.class.getSimpleName();
    private Face mFace = new Face();
    private int degrees;;
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
        int threadNum = 2;
        mFace.faceModelInit(sdPath, threadNum, minFaceSize);
    }

    public void stop(){
        if(mFace!=null){
            mFace.faceModelUnInit();
        }
    }

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
                double maxScore=0;
                int index=-1;
                for(int j =0; j<persions.size(); j++){
                    Persion persion = persions.get(j);
                    double score = mFace.faceRecognize(feature, persion.getFaceFeature());
                    if(score > maxScore){
                        index = j;
                    }
                }
                timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                Log.i(TAG, "recognize face time:"+timeDetectFace+"ms score"+maxScore);

                if(maxScore > Face.THRESHOLD){
                    paint.setColor(Color.GREEN);
                    paint.setTextSize(textSize);
                    paint.setStrokeWidth(3);
                    paint.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText(persions.get(index).getName(),faceInfos[i].getLeft(),faceInfos[i].getTop(),paint);
                }
            }
            cameraActivity.updateImageView(drawBitmap);
        }

//                    if((System.currentTimeMillis() - lastUpdate) > 5 * 1000){
//                        cameraActivity.updateImageView(bitmap);
//                        lastUpdate = System.currentTimeMillis();
//                    }
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
