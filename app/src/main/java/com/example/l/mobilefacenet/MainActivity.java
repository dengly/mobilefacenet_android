package com.example.l.mobilefacenet;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.l.mobilefacenet.util.FileUtil;
import com.example.l.mobilefacenet.util.ImageUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    private static final int SELECT_IMAGE1 = 1,SELECT_IMAGE2 = 2;
    private ImageView imageView1,imageView2;
    private Bitmap yourSelectedImage1 = null,yourSelectedImage2 = null;
    private Bitmap faceImage1 = null,faceImage2 = null;
    TextView faceInfo1,faceInfo2,cmpResult;
    private Face mFace = new Face();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);

        try {
            FileUtil.copyBigDataToSD(this.getAssets(), "det1.bin");
            FileUtil.copyBigDataToSD(this.getAssets(), "det2.bin");
            FileUtil.copyBigDataToSD(this.getAssets(), "det3.bin");
            FileUtil.copyBigDataToSD(this.getAssets(), "det1.param");
            FileUtil.copyBigDataToSD(this.getAssets(), "det2.param");
            FileUtil.copyBigDataToSD(this.getAssets(), "det3.param");
            FileUtil.copyBigDataToSD(this.getAssets(), "recognition.bin");
            FileUtil.copyBigDataToSD(this.getAssets(), "recognition.param");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //model init
        File sdDir = Environment.getExternalStorageDirectory();//get directory
        String sdPath = sdDir.toString() + "/facem/";
//        mFace.faceModelInit(sdPath, AndroidUtil.getNumberOfCPUCores()*2, Face.MIN_FACE_SIZE);
        mFace.faceModelInit(sdPath, 4, 80);

        //LEFT IMAGE
        imageView1 = (ImageView) findViewById(R.id.imageView1);
        faceInfo1=(TextView)findViewById(R.id.faceInfo1);
        Button buttonImage1 = (Button) findViewById(R.id.select1);
        buttonImage1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE1);
            }
        });

        Button buttonDetect1 = (Button) findViewById(R.id.detect1);
        buttonDetect1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage1 == null)
                    return;
                faceImage1=null;
                //detect
                int width = yourSelectedImage1.getWidth();
                int height = yourSelectedImage1.getHeight();
                byte[] imageDate = ImageUtil.getPixelsRGBA(yourSelectedImage1);

                long timeDetectFace = System.currentTimeMillis();
                Face.FaceInfo[] faceInfos = mFace.faceDetect(imageDate,width,height,Face.ColorType.R8G8B8A8);
                timeDetectFace = System.currentTimeMillis() - timeDetectFace;

                if(faceInfos !=null && faceInfos.length>0){
                    faceInfo1.setText("pic1 detect time:"+timeDetectFace);
                    Log.i(TAG, "pic width："+width+"height："+height+" face num：" + faceInfos.length );
                    Bitmap drawBitmap = yourSelectedImage1.copy(Bitmap.Config.ARGB_8888, true);
                    for(int i=0;i<faceInfos.length; i++) {
                        Canvas canvas = new Canvas(drawBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5);
                        canvas.drawRect(faceInfos[i].getLeft(), faceInfos[i].getTop(), faceInfos[i].getRight(), faceInfos[i].getBottom(), paint);
                        for(Point p : faceInfos[i].getPoints()){
                            paint.setColor(Color.RED);
                            canvas.drawPoint(p.x,p.y,paint);
                        }
                    }
                    imageView1.setImageBitmap(drawBitmap);
                    faceImage1 = Bitmap.createBitmap(yourSelectedImage1, faceInfos[0].getLeft(), faceInfos[0].getTop(), faceInfos[0].getWidth(), faceInfos[0].getHeight());
                }else{
                    faceInfo1.setText("no face");
                }
            }
        });

        //RIGHT IMAGE
        imageView2 = (ImageView) findViewById(R.id.imageView2);
        faceInfo2=(TextView)findViewById(R.id.faceInfo2);
        Button buttonImage2 = (Button) findViewById(R.id.select2);
        buttonImage2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE2);
            }
        });

        Button buttonDetect2 = (Button) findViewById(R.id.detect2);
        buttonDetect2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage2 == null)
                    return;
                //detect
                faceImage2=null;
                int width = yourSelectedImage2.getWidth();
                int height = yourSelectedImage2.getHeight();
                byte[] imageDate = ImageUtil.getPixelsRGBA(yourSelectedImage2);

                long timeDetectFace = System.currentTimeMillis();
                Face.FaceInfo[] faceInfos = mFace.faceDetect(imageDate,width,height,Face.ColorType.R8G8B8A8);
                timeDetectFace = System.currentTimeMillis() - timeDetectFace;

                if(faceInfos !=null && faceInfos.length>0){
                    faceInfo2.setText("pic2 detect time:"+timeDetectFace);
                    Log.i(TAG, "pic width："+width+"height："+height+" face num：" + faceInfos.length );
                    Bitmap drawBitmap = yourSelectedImage2.copy(Bitmap.Config.ARGB_8888, true);
                    for(int i=0;i<faceInfos.length; i++) {
                        Canvas canvas = new Canvas(drawBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5);
                        canvas.drawRect(faceInfos[i].getLeft(), faceInfos[i].getTop(), faceInfos[i].getRight(), faceInfos[i].getBottom(), paint);
                        for(Point p : faceInfos[i].getPoints()){
                            paint.setColor(Color.RED);
                            canvas.drawPoint(p.x,p.y,paint);
                        }
                    }
                    imageView2.setImageBitmap(drawBitmap);
                    faceImage2 = Bitmap.createBitmap(yourSelectedImage2, faceInfos[0].getLeft(), faceInfos[0].getTop(), faceInfos[0].getWidth(), faceInfos[0].getHeight());
                }else{
                    faceInfo2.setText("no face");
                }

            }
        });

        //cmp
        cmpResult=(TextView)findViewById(R.id.textView1);
        Button cmpImage = (Button) findViewById(R.id.facecmp);
        cmpImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (faceImage1 == null||faceImage2 == null){
                    cmpResult.setText("no enough face,return");
                    return;
                }
                byte[] faceDate1 = ImageUtil.getPixelsRGBA(faceImage1);
                byte[] faceDate2 = ImageUtil.getPixelsRGBA(faceImage2);

                long t0 = System.currentTimeMillis();
                float[] faceFeature1 = mFace.faceFeature(faceDate1,faceImage1.getWidth(),faceImage1.getHeight(),Face.ColorType.R8G8B8A8);
                long t1 = System.currentTimeMillis();
                float[] faceFeature2 = mFace.faceFeature(faceDate2,faceImage2.getWidth(),faceImage2.getHeight(),Face.ColorType.R8G8B8A8);
                long t2 = System.currentTimeMillis();
                double similar = mFace.faceRecognize(faceFeature1, faceFeature2);
                long t3 = System.currentTimeMillis();
                long timeRecognizeFace = t3 - t0;
                cmpResult.setText("特征码比对，cosin:"+similar+"\n"+"总 time:"+timeRecognizeFace
                        +"\n提取特征码1，time:"+(t1-t0)
                        +"\n提取特征码2，time:"+(t2-t1)
                        +"\n比对，time:"+(t3-t2));
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mFace!=null){
            mFace.faceModelUnInit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            try {
                if (requestCode == SELECT_IMAGE1) {
                    Bitmap bitmap = ImageUtil.decodeUri(this,selectedImage);
                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    yourSelectedImage1 = rgba;
                    imageView1.setImageBitmap(yourSelectedImage1);
                }
                else if (requestCode == SELECT_IMAGE2) {
                    Bitmap bitmap = ImageUtil.decodeUri(this,selectedImage);
                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    yourSelectedImage2 = rgba;
                    imageView2.setImageBitmap(yourSelectedImage2);
                }
            } catch (FileNotFoundException e) {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
    }

}
