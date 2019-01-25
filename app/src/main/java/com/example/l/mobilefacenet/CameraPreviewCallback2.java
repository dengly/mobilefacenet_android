package com.example.l.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.widget.ListView;

import com.example.l.mobilefacenet.model.FacePersion;
import com.example.l.mobilefacenet.model.Persion;
import com.example.l.mobilefacenet.model.VideoFrame;
import com.example.l.mobilefacenet.util.ImageUtil;
import com.example.l.mobilefacenet.util.TrackUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class CameraPreviewCallback2 implements PreviewCallback {
    private String TAG;
    private Face mFace;
    private int degrees;
    private Matrix matrix;
    private int width = 960;
    private int height = 720;
    private CameraActivity cameraActivity;
    private ImageUtil.NV21ToBitmap nv21ToBitmap ;
    private int textSize = 40;

    private Map<Long, FacePersion> map = new ConcurrentHashMap<>();
    private Map<Long, FacePersion> knownMap = new ConcurrentHashMap<>();

    private Queue<VideoFrame> queueA = new LinkedBlockingQueue<>();
    private Queue<VideoFrame> queueB = new LinkedBlockingQueue<>();
    private Queue<VideoFrame> queueC = new LinkedBlockingQueue<>();

    private ExecutorService threadPoolA = Executors.newFixedThreadPool(2);
    private ExecutorService threadPoolB = Executors.newFixedThreadPool(4);
    private ExecutorService threadPoolC = Executors.newFixedThreadPool(2);
    private ExecutorService threadPoolDisplay = Executors.newSingleThreadExecutor();

    private Face.ColorType colorType = Face.ColorType.R8G8B8A8;
    private boolean stop = false;
    private AtomicLong track = new AtomicLong(0);

    private List<Persion> persions;

    public CameraPreviewCallback2(){
        startService();
    }

    public void stop(){
        stop = true;
    }

    private long getTrack(){
        return track.incrementAndGet();
    }

    private void startService(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop){
                    if(queueA.size() == 0){
                        try {
                            queueA.wait(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(queueA.size() > 0){
                        threadPoolA.execute(new Runnable() {
                            @Override
                            public void run() {
                                // 转换视频帧数据 格式、旋转等
                                VideoFrame videoFrame = queueA.remove();
                                Bitmap bitmap ;
                                int _degrees = cameraActivity.getCameraId() == Camera.CameraInfo.CAMERA_FACING_BACK ? degrees : degrees + 90;
                                if(_degrees!=0){
                                    if(matrix==null){
                                        matrix = new Matrix();
                                        matrix.setRotate(_degrees, width/2, height/2);
                                    }
                                    bitmap = nv21ToBitmap.nv21ToBitmap(videoFrame.frame, width, height, matrix);
                                }else{
                                    bitmap = nv21ToBitmap.nv21ToBitmap(videoFrame.frame, width, height);
                                }
                                videoFrame.bitmap = bitmap;
                                videoFrame.frame = ImageUtil.getPixelsRGBA(bitmap);
                                // 添加到队列B中，并通知队列B相关线程
                                queueB.add(videoFrame);
                                queueB.notify();
                            }
                        });
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop){
                    if(queueB.size() == 0){
                        try {
                            queueB.wait(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(queueB.size() > 0){
                        threadPoolB.execute(new Runnable() {
                            @Override
                            public void run() {
                                VideoFrame videoFrame = queueB.remove();
                                // 人脸检测
                                long timeDetectFace = System.currentTimeMillis();
                                Face.FaceInfo[] faceInfos = mFace.faceDetect(videoFrame.frame,videoFrame.width,videoFrame.height,colorType);
                                timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                                Log.i(TAG, "detect face time:"+timeDetectFace+"ms");
                                if(faceInfos !=null && faceInfos.length>0){
                                    long time = System.currentTimeMillis();
                                    boolean empty = map.isEmpty() && knownMap.isEmpty();
                                    for(Face.FaceInfo faceInfo : faceInfos){
                                        if(empty){
                                            // 新进人脸
                                            addMap(videoFrame, faceInfo, time);
                                        }else{
                                            boolean has = false;
                                            if(!knownMap.isEmpty()){
                                                Set<Map.Entry<Long, FacePersion>> set = knownMap.entrySet();
                                                for(Map.Entry<Long, FacePersion> entry : set){
                                                    if(TrackUtil.isSameFace(TrackUtil.SIMILARITY_RECT,faceInfo, entry.getValue().faceInfo)){
                                                        if(videoFrame.trackMap==null){
                                                            videoFrame.trackMap = new HashMap<>();
                                                        }
                                                        videoFrame.trackMap.put(entry.getKey(), faceInfo);
                                                        has = true;
                                                        entry.getValue().time = time;
                                                        break;
                                                    }
                                                }
                                            }
                                            if(!has && !map.isEmpty()){
                                                Set<Map.Entry<Long, FacePersion>> set = map.entrySet();
                                                for(Map.Entry<Long, FacePersion> entry : set){
                                                    if(TrackUtil.isSameFace(TrackUtil.SIMILARITY_RECT,faceInfo, entry.getValue().faceInfo)){
                                                        if(videoFrame.trackMap==null){
                                                            videoFrame.trackMap = new HashMap<>();
                                                        }
                                                        videoFrame.trackMap.put(entry.getKey(), faceInfo);
                                                        has = true;
                                                        entry.getValue().time = time;
                                                        break;
                                                    }
                                                }
                                            }
                                            if(!has){
                                                // 新进人脸
                                                addMap(videoFrame, faceInfo, time);
                                            }
                                        }
                                    }
                                    if(!empty){
                                        // 删除离开的人脸
                                        if(!knownMap.isEmpty()){
                                            Set<Map.Entry<Long, FacePersion>> set = knownMap.entrySet();
                                            for(Map.Entry<Long, FacePersion> entry : set){
                                                if(entry.getValue().time != time){
                                                    knownMap.remove(entry.getKey());
                                                }
                                            }
                                        }
                                        if(!map.isEmpty()){
                                            Set<Map.Entry<Long, FacePersion>> set = map.entrySet();
                                            for(Map.Entry<Long, FacePersion> entry : set){
                                                if(entry.getValue().time != time){
                                                    knownMap.remove(entry.getKey());
                                                }
                                            }
                                        }
                                    }
                                }
                                // 添加到队列C中，并通知队列C相关线程
                                queueC.add(videoFrame);
                                queueC.notify();
                            }
                        });
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop){
                    if(queueC.size() == 0){
                        try {
                            queueC.wait(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        threadPoolDisplay.execute(new Runnable() {
                            @Override
                            public void run() {
                                VideoFrame videoFrame = queueC.remove();
                                // 显示
                                if(videoFrame.trackMap!=null && !videoFrame.trackMap.isEmpty()){
                                    Set<Map.Entry<Long, Face.FaceInfo>> set = videoFrame.trackMap.entrySet();

                                    Canvas canvas = new Canvas(videoFrame.bitmap);
                                    Paint paint = new Paint();
                                    paint.setColor(Color.BLUE);
                                    paint.setStyle(Paint.Style.STROKE);
                                    paint.setStrokeWidth(5);

                                    for(Map.Entry<Long, Face.FaceInfo> entry : set){
                                        Face.FaceInfo faceInfo = entry.getValue();
                                        canvas.drawRect(faceInfo.getLeft(), faceInfo.getTop(), faceInfo.getRight(), faceInfo.getBottom(), paint);

                                        FacePersion facePersion = knownMap.get(entry.getKey());
                                        if(facePersion!=null){
                                            paint.setColor(Color.GREEN);
                                            paint.setTextSize(textSize);
                                            paint.setStrokeWidth(3);
                                            paint.setTextAlign(Paint.Align.LEFT);
                                            canvas.drawText(String.format("%s-%.3f",facePersion.name,facePersion.score), faceInfo.getLeft(), faceInfo.getTop(),paint);
                                        }
                                    }
                                }
                                cameraActivity.updateImageView(videoFrame.bitmap);
                            }
                        });
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop){
                    if(map.size() == 0){
                        try {
                            map.wait(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        if(!map.isEmpty()){
                            Set<Map.Entry<Long, FacePersion>> set = map.entrySet();
                            for(Map.Entry<Long, FacePersion> entry : set){
                                final FacePersion facePersion = map.remove(entry.getKey());
                                threadPoolC.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 人脸特征提取及比对
                                        long timeDetectFace = System.currentTimeMillis();
                                        float[] feature = mFace.faceFeature(facePersion.faceDate,facePersion.faceInfo.getWidth(),facePersion.faceInfo.getHeight(),colorType);
                                        if(persions!=null && persions.size()>0){
                                            facePersion.score = 0;
                                            for(Persion persion : persions){
                                                double score = mFace.faceRecognize(feature, persion.getFaceFeature());
                                                if(score > facePersion.score){
                                                    facePersion.name = persion.getName();
                                                    facePersion.score = score;
                                                }
                                            }
                                            if(facePersion.score < Face.THRESHOLD){
                                                facePersion.name = "未知";
                                            }
                                            knownMap.put(facePersion.trackId, facePersion);
                                        }
                                        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                                        Log.i(TAG, "recognize face time:"+timeDetectFace+"ms");
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private void addMap(VideoFrame videoFrame, Face.FaceInfo faceInfo, long time){
        Bitmap faceImage = Bitmap.createBitmap(videoFrame.bitmap, faceInfo.getLeft(), faceInfo.getTop(), faceInfo.getWidth(), faceInfo.getHeight());
        byte[] faceDate = ImageUtil.getPixelsRGBA(faceImage);
        long trackId = getTrack();
        FacePersion facePersion = new FacePersion();
        facePersion.trackId = trackId;
        facePersion.faceInfo = faceInfo;
        facePersion.faceDate = faceDate;
        facePersion.time = time;
        if(videoFrame.trackMap==null){
            videoFrame.trackMap = new HashMap<>();
        }
        videoFrame.trackMap.put(trackId, faceInfo);
        map.put(facePersion.trackId, facePersion);
        map.notify();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        VideoFrame videoFrame = new VideoFrame();
        videoFrame.time = System.currentTimeMillis();
        videoFrame.frame = data;
        videoFrame.width = width;
        videoFrame.height = height;
        // 将视频帧添加到队列A中，并通知队列A相关线程
        queueA.add(videoFrame);
        queueA.notify();
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

    public void setPersions(List<Persion> persions) {
        this.persions = persions;
    }
}
