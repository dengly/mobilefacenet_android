package com.example.l.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;


/**
 * 参考 https://www.jianshu.com/p/7dd2191b4537
 */
public abstract class BitmapCallback implements Camera.PictureCallback {
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        onPictureTaken(BitmapFactory.decodeByteArray(data, 0, data.length));
    }
    public abstract void onPictureTaken(Bitmap bitmap);
}
