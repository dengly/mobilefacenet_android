package com.example.l.mobilefacenet;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.example.l.mobilefacenet.model.Persion;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private static String[] PERMISSIONS_CAMERA = {
            "android.permission.CAMERA"};

    private LiveCameraView liveCameraView;
    private ImageView imageView;
    private int cameraId;
    private int faceType;

    private Persion persion;

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

    public Persion getPersion(){
        return persion;
    }

    public int getCameraId(){
        return cameraId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imageView = findViewById(R.id.imageView);
        liveCameraView = findViewById(R.id.liveCameraView);
        verifyStoragePermissions(this);

        persion = (Persion)getIntent().getParcelableExtra("persion");
        cameraId = getIntent().getIntExtra("cameraId", Camera.CameraInfo.CAMERA_FACING_BACK);
        faceType = getIntent().getIntExtra("faceType", 1);


        Camera camera = CameraHelper.openCamera(cameraId);
        if(camera!=null){
            liveCameraView.setFaceType(faceType);
            liveCameraView.setCamera(camera);
            liveCameraView.setActivity(CameraActivity.this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        liveCameraView.releaseCamera();
        this.finish();
    }

    public void updateImageView(final Bitmap bitmap){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liveCameraView.stop();
    }
}
