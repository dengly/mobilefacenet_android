package com.example.l.mobilefacenet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.widget.ImageView;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.example.l.mobilefacenet.model.FacePersion;
import com.example.l.mobilefacenet.model.Persion;
import com.example.l.mobilefacenet.model.VideoFrame;
import com.example.l.mobilefacenet.util.ImageUtil;
import com.example.l.mobilefacenet.util.TrackUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CameraPreviewCallback3 implements AbstractCameraPreviewCallback {
    private final static String TAG = LiveCameraView.class.getSimpleName();
    private int degrees;
    private Matrix matrix;
    private int width = 640;
    private int height = 480;
    private CameraActivity cameraActivity;
    private ImageUtil.NV21ToBitmap nv21ToBitmap ;
    private int textSize = 40;

    private static final int FACE_DETECT_NUM = 4;
    private static final int FACE_RECOGNIZE_NUM = 2;

    private Queue<FaceEngine> queueFaceDetect = new ArrayBlockingQueue<>(FACE_DETECT_NUM+1);
    private Queue<FaceEngine> queueFaceRecognize = new ArrayBlockingQueue<>(FACE_RECOGNIZE_NUM+1);

    private Map<Long, FacePersion> map = new HashMap<>();
    private Map<Long, FacePersion> knownMap = new HashMap<>();

    private Queue<VideoFrame> queueB = new LinkedList<>();
    private Queue<VideoFrame> queueC = new LinkedList<>();

    private ExecutorService threadPoolB = Executors.newFixedThreadPool(FACE_DETECT_NUM);
    private ExecutorService threadPoolC = Executors.newFixedThreadPool(FACE_RECOGNIZE_NUM);

    private static final int colorType = FaceEngine.CP_PAF_NV21;
    private boolean stop = false;
    private AtomicLong track = new AtomicLong(0);

    private List<Persion> persions;

    private AtomicInteger initNum = new AtomicInteger(0);

    public CameraPreviewCallback3(Context context){
        for(int i=0; i<FACE_DETECT_NUM+1; i++){
            FaceEngine faceEngine = new FaceEngine();
            int engineCode = faceEngine.init(context, FaceEngine.ASF_DETECT_MODE_VIDEO, FaceEngine.ASF_OP_0_HIGHER_EXT, 16, 10, FaceEngine.ASF_FACE_DETECT);
            if (engineCode == ErrorInfo.MOK) {
                queueFaceDetect.offer(faceEngine);
            }else{
                Log.e(TAG, "人脸检测引擎初始化失败");
            }
        }
        for(int i=0; i<FACE_RECOGNIZE_NUM+1; i++){
            FaceEngine faceEngine = new FaceEngine();
            int engineCode = faceEngine.init(context, FaceEngine.ASF_DETECT_MODE_VIDEO, FaceEngine.ASF_OP_0_HIGHER_EXT, 16, 10, FaceEngine.ASF_FACE_RECOGNITION);
            if (engineCode == ErrorInfo.MOK) {
                queueFaceRecognize.offer(faceEngine);
            }else{
                Log.e(TAG, "人脸识别引擎初始化失败");
            }
        }
        startService();
    }

    public void stop(){
        stop = true;
    }

    public long getTrack(){
        return track.incrementAndGet();
    }

    private void startService(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                initNum.incrementAndGet();
                while(!stop){
                    synchronized (queueB){
                        if(queueB.size() == 0){
                            try {
                                queueB.wait(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(!queueB.isEmpty()){
                        VideoFrame tempVideoFrame = null;
                        try{
                            tempVideoFrame = queueB.poll();
                        }catch (Exception e){
                            tempVideoFrame = null;
                        }
                        final VideoFrame videoFrame = tempVideoFrame ;
                        if(videoFrame!=null && videoFrame.frame != null && videoFrame.frame.length>0){
                            threadPoolB.execute(new Runnable() {
                                @Override
                                public void run() {
                                    // 人脸检测
                                    long timeDetectFace = System.currentTimeMillis();
                                    FaceEngine faceEngine = queueFaceDetect.poll();
                                    if(faceEngine==null){
                                        synchronized (queueC){
                                            // 添加到队列C中，并通知队列C相关线程
                                            queueC.offer(videoFrame);
                                            queueC.notifyAll();
                                        }
                                        return;
                                    }
                                    List<FaceInfo> faceInfos = new ArrayList<>();
                                    faceEngine.detectFaces(videoFrame.frame, width, height, colorType, faceInfos);
                                    timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                                    Log.i(TAG, "detect face time:"+timeDetectFace+"ms faceInfos.size:"+faceInfos.size());
                                    if(faceInfos !=null && faceInfos.size()>0){
                                        final long time = System.currentTimeMillis();
                                        boolean empty = map.isEmpty() && knownMap.isEmpty();
                                        for(FaceInfo faceInfo : faceInfos){
                                            if(empty){
                                                // 新进人脸
                                                addMap(videoFrame, faceInfo, time);
                                            }else{
                                                boolean has = false;
                                                synchronized (knownMap){
                                                    if(!knownMap.isEmpty()){
                                                        has = hasSameFace(knownMap, videoFrame,faceInfo, time);
                                                    }
                                                }
                                                synchronized (map){
                                                    if(!has && !map.isEmpty()){
                                                        has = hasSameFace(map, videoFrame,faceInfo, time);
                                                    }
                                                }
                                                if(!has){
                                                    // 新进人脸
                                                    addMap(videoFrame, faceInfo, time);
                                                }
                                            }
                                        }
                                        synchronized (queueC){
                                            // 添加到队列C中，并通知队列C相关线程
                                            queueC.offer(videoFrame);
                                            queueC.notifyAll();
                                        }
                                        if(!empty){
                                            // 删除离开的人脸
                                            synchronized (knownMap){
                                                if(!knownMap.isEmpty()){
                                                    for (Iterator<Map.Entry<Long, FacePersion>> it = knownMap.entrySet().iterator(); it.hasNext();){
                                                        Map.Entry<Long, FacePersion> item = it.next();
                                                        if(item.getValue().time != time){
                                                            it.remove();
                                                        }
                                                    }
                                                }
                                            }
                                            synchronized (map){
                                                if(!map.isEmpty()){
                                                    for (Iterator<Map.Entry<Long, FacePersion>> it = map.entrySet().iterator(); it.hasNext();){
                                                        Map.Entry<Long, FacePersion> item = it.next();
                                                        if(item.getValue().time != time){
                                                            it.remove();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }else{
                                        synchronized (knownMap){
                                            knownMap.clear();
                                        }
                                        synchronized (map){
                                            map.clear();
                                        }
                                        synchronized (queueC){
                                            // 添加到队列C中，并通知队列C相关线程
                                            queueC.offer(videoFrame);
                                            queueC.notifyAll();
                                        }
                                    }
                                    queueFaceDetect.offer(faceEngine);
                                }
                            });
                        }
                    }
                }
                int size = queueFaceDetect.size();
                for(int i=0; i<size; i++){
                    FaceEngine faceEngine = queueFaceDetect.poll();
                    faceEngine.unInit();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stop){
                    initNum.incrementAndGet();
                    synchronized (queueC){
                        if(queueC.isEmpty()){
                            try {
                                queueC.wait(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(!queueC.isEmpty()){
                        VideoFrame tempVideoFrame = null;
                        try{
                            tempVideoFrame = queueC.poll();
                        }catch (Exception e){
                            tempVideoFrame = null;
                        }
                        final VideoFrame videoFrame = tempVideoFrame ;
                        if(videoFrame!=null && videoFrame.frame != null){
                            // 显示
                            videoFrame.bitmap = nv21ToBitmap.nv21ToBitmap(videoFrame.frame, width, height);
                            videoFrame.frame = null;
                            if(videoFrame.arcsoftTrackMap!=null && !videoFrame.arcsoftTrackMap.isEmpty()){
                                Canvas canvas = new Canvas(videoFrame.bitmap);
                                Paint paint = new Paint();
                                paint.setColor(Color.BLUE);
                                paint.setStyle(Paint.Style.STROKE);
                                paint.setStrokeWidth(5);

                                for (Iterator<Map.Entry<Long, FaceInfo>> it = videoFrame.arcsoftTrackMap.entrySet().iterator(); it.hasNext();){
                                    Map.Entry<Long, FaceInfo> entry = it.next();
                                    FaceInfo faceInfo = entry.getValue();
                                    canvas.drawRect(faceInfo.getRect(), paint);

                                    FacePersion facePersion ;
                                    synchronized (knownMap){
                                        facePersion = knownMap.get(entry.getKey());
                                    }
                                    if(facePersion!=null){
                                        paint.setColor(Color.GREEN);
                                        paint.setTextSize(textSize);
                                        paint.setStrokeWidth(3);
                                        paint.setTextAlign(Paint.Align.LEFT);
                                        canvas.drawText(String.format("%s-%.3f",facePersion.name,facePersion.score), faceInfo.getRect().left, faceInfo.getRect().top,paint);
                                    }
                                }
                            }
                            cameraActivity.updateImageView(videoFrame.bitmap);
                        }
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                initNum.incrementAndGet();
                while(!stop){
                    synchronized (map){
                        if(map.size() == 0){
                            try {
                                map.wait(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if(!map.isEmpty()){
                            for (Iterator<Map.Entry<Long, FacePersion>> it = map.entrySet().iterator(); it.hasNext();){
                                Map.Entry<Long, FacePersion> item = it.next();
                                final FacePersion facePersion = item.getValue();
                                threadPoolC.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 人脸特征提取及比对
                                        long timeDetectFace = System.currentTimeMillis();
                                        FaceEngine faceEngine = queueFaceRecognize.poll();
                                        if(faceEngine==null){
                                            return;
                                        }
                                        FaceFeature feature =  new FaceFeature();
                                        faceEngine.extractFaceFeature(facePersion.faceDate, facePersion.faceDateW, facePersion.faceDateH, colorType, facePersion.arcsoftFaceInfo, feature);
                                        facePersion.faceDate = null;
                                        if(feature==null || feature.getFeatureData()==null || feature.getFeatureData().length==0){
                                            queueFaceRecognize.offer(faceEngine);
                                            return;
                                        }
                                        if(persions!=null && persions.size()>0){
                                            facePersion.score = 0;
                                            for(Persion persion : persions){
                                                FaceSimilar faceSimilar = new FaceSimilar();
                                                faceEngine.compareFaceFeature(feature, new FaceFeature(persion.getFeatureData()), faceSimilar);
                                                if(faceSimilar.getScore() > facePersion.score){
                                                    facePersion.name = persion.getName();
                                                    facePersion.score = faceSimilar.getScore();
                                                }
                                            }
                                            if(facePersion.score < Face.THRESHOLD){
                                                facePersion.name = "未知";
                                            }
                                            synchronized (knownMap){
                                                knownMap.put(facePersion.trackId, facePersion);
                                            }
                                        }
                                        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                                        queueFaceRecognize.offer(faceEngine);
                                        Log.i(TAG, "recognize face time:"+timeDetectFace+"ms");
                                    }
                                });
                                it.remove();
                            }
                        }
                    }
                }
                int size = queueFaceRecognize.size();
                for(int i=0; i<size; i++){
                    FaceEngine faceEngine = queueFaceRecognize.poll();
                    faceEngine.unInit();
                }
            }
        }).start();
    }

    /**
     * 是否存在相同人脸
     * @param tempMap
     * @param videoFrame
     * @param faceInfo
     * @param time
     * @return
     */
    private boolean hasSameFace(final Map<Long, FacePersion> tempMap, final VideoFrame videoFrame, final FaceInfo faceInfo, final long time){
        boolean has = false;
        for(Iterator<Map.Entry<Long, FacePersion>> it = tempMap.entrySet().iterator(); it.hasNext();){
            Map.Entry<Long, FacePersion> entry = it.next();
            if(TrackUtil.isSameFace(TrackUtil.SIMILARITY_RECT, faceInfo.getRect(), entry.getValue().arcsoftFaceInfo.getRect())){
                // 粗暴处理，同一张人脸在两帧之间的位置变化不大，且人脸面积变化也不大。即两帧之间同一个区域基本上是同一张人脸。
                if(videoFrame.arcsoftTrackMap==null){
                    videoFrame.arcsoftTrackMap = new HashMap<>();
                }
                videoFrame.arcsoftTrackMap.put(entry.getKey(), faceInfo);
                has = true;
                entry.getValue().time = time;
                break;
            }
        }
        return has;
    }

    private void addMap(VideoFrame videoFrame, FaceInfo faceInfo, long time){
        long trackId = getTrack();
        FacePersion facePersion = new FacePersion();
        facePersion.trackId = trackId;
        facePersion.faceDate = videoFrame.frame;
        facePersion.arcsoftFaceInfo = faceInfo;
        facePersion.faceDateW = width;
        facePersion.faceDateH = height;
        facePersion.time = time;
        if(videoFrame.arcsoftTrackMap==null){
            videoFrame.arcsoftTrackMap = new HashMap<>();
        }
        videoFrame.arcsoftTrackMap.put(trackId, faceInfo);
        synchronized (map){
            map.put(facePersion.trackId, facePersion);
            map.notifyAll();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
        if(data==null || data.length<=0 || initNum.get()<3){
            return;
        }
        VideoFrame videoFrame = new VideoFrame();
        videoFrame.time = System.currentTimeMillis();
        videoFrame.frame = Arrays.copyOf(data,data.length);
        videoFrame.width = width;
        videoFrame.height = height;

        // 控制帧频
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 添加到队列B中，并通知队列B相关线程
        synchronized (queueB) {
            queueB.offer(videoFrame);
            queueB.notifyAll();
        }

        Log.i(TAG, "onPreviewFrame: "+videoFrame.time+" ms");
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
