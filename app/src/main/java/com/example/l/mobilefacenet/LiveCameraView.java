package com.example.l.mobilefacenet;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.l.mobilefacenet.model.Persion;
import com.example.l.mobilefacenet.util.ImageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private CameraActivity cameraActivity;

//    CameraPreviewCallback cameraPreviewCallback = new CameraPreviewCallback();
    CameraPreviewCallback2 cameraPreviewCallback = new CameraPreviewCallback2();

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
    }

    public void setActivity(CameraActivity cameraActivity) {
        this.cameraActivity = cameraActivity;
    }

    public void stop(){
        cameraPreviewCallback.stop();
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

            cameraPreviewCallback.setCameraActivity(cameraActivity);
            cameraPreviewCallback.setDegrees(degrees);
            cameraPreviewCallback.setHeight(height);
            cameraPreviewCallback.setNv21ToBitmap(nv21ToBitmap);
            cameraPreviewCallback.setTextSize(textSize);
            cameraPreviewCallback.setWidth(width);
            List<Persion> list = new ArrayList<>();
            list.add(cameraActivity.getPersion());
            cameraPreviewCallback.setPersions(list);

//            mCamera.setPreviewCallback(cameraPreviewCallback);
            mCamera.addCallbackBuffer(new byte[((width * height) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8]);
            mCamera.setPreviewCallbackWithBuffer(cameraPreviewCallback);

            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error while START preview for camera", e);
        }
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
