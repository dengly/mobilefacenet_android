package com.example.l.mobilefacenet;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;

import java.util.List;

public class CameraHelper {

    public static int getNumberOfCameras(){
        return Camera.getNumberOfCameras();
    }

    public static Camera openCamera(int cameraId) {
        try{
            return Camera.open(cameraId);
        }catch(Exception e) {
            return null;
        }
    }

    public static void followScreenOrientation(Context context, Camera camera){
        final int orientation = context.getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            camera.setDisplayOrientation(180);
        }else if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            camera.setDisplayOrientation(90);
        }
    }

    /**
     * 判断手机设备是否有相机设备
     * @param ctx
     * @return
     */
    public static boolean hasCameraDevice(Context ctx) {
        return ctx.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * 判断是否支持自动对焦
     * @param params
     * @return
     */
    public static boolean isAutoFocusSupported(Camera.Parameters params) {
        List<String> modes = params.getSupportedFocusModes();
        return modes.contains(Camera.Parameters.FOCUS_MODE_AUTO);
    }
}
