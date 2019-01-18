package com.example.l.mobilefacenet;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.l.mobilefacenet.util.AndroidUtil;

public class CameraActivity extends AppCompatActivity {
    private Face mFace = new Face();
    private static final int REQUEST_CAMERA = 1;
    private static String[] PERMISSIONS_CAMERA = {
            "android.permission.CAMERA"};

    private LiveCameraView liveCameraView;

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_CAMERA, REQUEST_CAMERA);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        liveCameraView = findViewById(R.id.liveCameraView);
        verifyStoragePermissions(this);

        if(CameraHelper.hasCameraDevice(this)){
            Camera camera = CameraHelper.openCamera(0);
            liveCameraView.setCamera(camera);
//            camera.setPreviewCallback(new BitmapCallback() {
//                @Override
//                public void onPictureTaken(Bitmap bitmap) {
//                    ;
//                }
//            });
        }
    }
}
