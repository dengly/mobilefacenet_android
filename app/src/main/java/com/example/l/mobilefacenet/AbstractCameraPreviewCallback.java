package com.example.l.mobilefacenet;

import android.hardware.Camera;

import com.example.l.mobilefacenet.model.Persion;
import com.example.l.mobilefacenet.util.ImageUtil;

import java.util.List;

public interface AbstractCameraPreviewCallback extends Camera.PreviewCallback {
    void stop();

    void setDegrees(int degrees);

    void setWidth(int width);

    void setHeight(int height);

    void setCameraActivity(CameraActivity cameraActivity) ;

    void setNv21ToBitmap(ImageUtil.NV21ToBitmap nv21ToBitmap) ;

    void setTextSize(int textSize);

    void setPersions(List<Persion> persions);
}
