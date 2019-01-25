package com.example.l.mobilefacenet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.l.mobilefacenet.util.CommonUtil;
import com.example.l.mobilefacenet.util.ImageUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 参考 https://www.jianshu.com/p/7dd2191b4537
 */
public class LiveCameraView extends SurfaceView implements SurfaceHolder.Callback {

    private final static String TAG = LiveCameraView.class.getSimpleName();
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;

    private int textSize = 40;
    private ImageUtil.NV21ToBitmap nv21ToBitmap ;
    private int width = 960;
    private int height = 720;
    private int degrees;
    private Matrix matrix;
    private Face mFace = new Face();
    private CameraActivity cameraActivity;
    private long lastUpdate = System.currentTimeMillis();
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public LiveCameraView(Context context) {
        this(context, null);
    }

    public LiveCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiveCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        nv21ToBitmap = new ImageUtil.NV21ToBitmap(context);

        //model init
        File sdDir = Environment.getExternalStorageDirectory();//get directory
        String sdPath = sdDir.toString() + "/facem/";
//        mFace.faceModelInit(sdPath, AndroidUtil.getNumberOfCPUCores()*2, Face.MIN_FACE_SIZE);
//        int minFaceSize = CommonUtil.max(width, height) / Face.MIN_FACE_SIZE_SIDE_SCALE;
        int minFaceSize = CommonUtil.sqrt(CommonUtil.area(width, height) / Face.MIN_FACE_SIZE_SCALE);
        int threadNum = 2;
        mFace.faceModelInit(sdPath, threadNum, minFaceSize);
        Log.d(TAG, "face threadNum:"+threadNum+" minFaceSize:" + minFaceSize);
    }

    public void setActivity(CameraActivity cameraActivity) {
        this.cameraActivity = cameraActivity;
    }

    public void stop(){
        if(mFace!=null){
            mFace.faceModelUnInit();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Start preview display[SURFACE-CREATED]");
        startPreviewDisplay(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mSurfaceHolder.getSurface() == null){
            return;
        }
        degrees = CameraHelper.followScreenOrientation(getContext(), mCamera);
        Log.d(TAG, "Restart preview display[SURFACE-CHANGED] width:"+width+" height:"+height);
//        stopPreviewDisplay();
        startPreviewDisplay(mSurfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Stop preview display[SURFACE-DESTROYED]");
        stopPreviewDisplay();
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        final Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO); // 自动模式，当光线较暗时自动打开闪光灯；
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 自动对焦模式，摄影小白专用模式；
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO); // 自动选择场景；
        params.setPreviewSize(width,height);
        params.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(params);
    }

    private void startPreviewDisplay(SurfaceHolder holder){
        checkCamera();
        try {
            mCamera.setPreviewDisplay(holder);

            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
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
            });

            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error while START preview for camera", e);
        }
    }

    private byte[] bitmap2Bytes(Bitmap bm) {
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    private void stopPreviewDisplay(){
        checkCamera();
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            Log.e(TAG, "Error while STOP preview for camera", e);
        }
    }

    private void checkCamera(){
        if(mCamera == null) {
            throw new IllegalStateException("Camera must be set when start/stop preview, call <setCamera(Camera)> to set");
        }
    }
}
