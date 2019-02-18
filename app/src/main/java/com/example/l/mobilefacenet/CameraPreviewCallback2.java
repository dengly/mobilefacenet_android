package com.example.l.mobilefacenet;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import com.example.l.mobilefacenet.model.FacePersion;
import com.example.l.mobilefacenet.model.Persion;
import com.example.l.mobilefacenet.model.VideoFrame;
import com.example.l.mobilefacenet.util.CommonUtil;
import com.example.l.mobilefacenet.util.ImageUtil;
import com.example.l.mobilefacenet.util.TrackUtil;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CameraPreviewCallback2 implements AbstractCameraPreviewCallback {
    private final static String TAG = LiveCameraView.class.getSimpleName();
    private int degrees;
    private Matrix matrix;
    private int width = 640;
    private int height = 480;
    private CameraActivity cameraActivity;
    private ImageUtil.NV21ToBitmap nv21ToBitmap ;
    private int textSize = 40;

    private static final int FACE_DETECT_NUM = 1;
    private static final int FACE_RECOGNIZE_NUM = 1;
    private static final int waitTime = 10;

    private Face mFace;

    private Map<Long, FacePersion> map = new HashMap<>();
    private Map<Long, FacePersion> knownMap = new HashMap<>();

    private Queue<VideoFrame> queueB = new LinkedList<>();
    private Queue<VideoFrame> queueC = new LinkedList<>();

    private ExecutorService threadPoolB = Executors.newFixedThreadPool(FACE_DETECT_NUM);
    private ExecutorService threadPoolC = Executors.newFixedThreadPool(FACE_RECOGNIZE_NUM);

    private static final Face.ColorType colorType = Face.ColorType.NV21;
    private boolean stop = false;
    private AtomicLong track = new AtomicLong(0);

    private List<Persion> persions;

    private AtomicInteger initNum = new AtomicInteger(0);

    public CameraPreviewCallback2(){
        File sdDir = Environment.getExternalStorageDirectory();//get directory
        String sdPath = sdDir.toString() + "/facem/";
        int minFaceSize = CommonUtil.sqrt(CommonUtil.area(width, height) / Face.MIN_FACE_SIZE_SCALE);
        int threadNum = 2;
        mFace = new Face();
        mFace.faceModelInit(sdPath, threadNum, minFaceSize);
        startService();
    }

    public void stop(){
        stop = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(initNum.get()<=0){
                        mFace.faceModelUnInit();
                        mFace = null;
                        break;
                    }
                }
            }
        }).start();
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
                                queueB.wait(waitTime);
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
                        if(videoFrame!=null && videoFrame.frame != null){
                            threadPoolB.execute(new Runnable() {
                                @Override
                                public void run() {
                                    // 人脸检测
                                    long timeDetectFace = System.currentTimeMillis();
                                    Face.FaceInfo[] faceInfos = mFace.faceDetect(Face.DETECTTYPE_MTCNN, videoFrame.frame, videoFrame.width, videoFrame.height, colorType);
                                    timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                                    Log.i(TAG, "detect face time:"+timeDetectFace+"ms");
                                    if(faceInfos !=null && faceInfos.length>0){
                                        final long time = System.currentTimeMillis();
                                        boolean empty = map.isEmpty() && knownMap.isEmpty();
                                        for(Face.FaceInfo faceInfo : faceInfos){
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
                                }
                            });
                        }
                    }
                }
                initNum.decrementAndGet();
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
                                queueC.wait(waitTime);
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
                            videoFrame.bitmap = nv21ToBitmap.nv21ToBitmap(videoFrame.frame, width, height);
                            videoFrame.frame = null;
                            // 显示
                            if(videoFrame.trackMap!=null && !videoFrame.trackMap.isEmpty()){
                                Canvas canvas = new Canvas(videoFrame.bitmap);
                                Paint paint = new Paint();
                                paint.setColor(Color.BLUE);
                                paint.setStyle(Paint.Style.STROKE);
                                paint.setStrokeWidth(5);

                                for (Iterator<Map.Entry<Long, Face.FaceInfo>> it = videoFrame.trackMap.entrySet().iterator(); it.hasNext();){
                                    Map.Entry<Long, Face.FaceInfo> entry = it.next();
                                    Face.FaceInfo faceInfo = entry.getValue();
                                    canvas.drawRect(faceInfo.getLeft(), faceInfo.getTop(), faceInfo.getRight(), faceInfo.getBottom(), paint);

                                    FacePersion facePersion ;
                                    synchronized (knownMap){
                                        facePersion = knownMap.get(entry.getKey());
                                    }
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
                    }
                }
                initNum.decrementAndGet();
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
                                map.wait(waitTime);
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
                                        float[] feature = mFace.faceFeature(facePersion.faceDate, facePersion.faceDateW, facePersion.faceDateH, colorType);
                                        facePersion.faceDate = null;
                                        if(feature==null || feature.length==0){
                                            return;
                                        }
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
                                            synchronized (knownMap){
                                                knownMap.put(facePersion.trackId, facePersion);
                                            }
                                        }
                                        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                                        Log.i(TAG, "recognize face time:"+timeDetectFace+"ms");
                                    }
                                });
                                it.remove();
                            }
                        }
                    }
                }
                initNum.decrementAndGet();
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
    private boolean hasSameFace(final Map<Long, FacePersion> tempMap, final VideoFrame videoFrame, final Face.FaceInfo faceInfo, final long time){
        boolean has = false;
        for(Iterator<Map.Entry<Long, FacePersion>> it = tempMap.entrySet().iterator(); it.hasNext();){
            Map.Entry<Long, FacePersion> entry = it.next();
            if(TrackUtil.isSameFace(TrackUtil.SIMILARITY_RECT, faceInfo, entry.getValue().faceInfo)){
                // 粗暴处理，同一张人脸在两帧之间的位置变化不大，且人脸面积变化也不大。即两帧之间同一个区域基本上是同一张人脸。
                if(videoFrame.trackMap==null){
                    videoFrame.trackMap = new HashMap<>();
                }
                videoFrame.trackMap.put(entry.getKey(), faceInfo);
                has = true;
                entry.getValue().time = time;
                break;
            }
        }
        return has;
    }

    private void addMap(VideoFrame videoFrame, Face.FaceInfo faceInfo, long time){
        long trackId = getTrack();
        FacePersion facePersion = new FacePersion();
        facePersion.trackId = trackId;
        facePersion.faceInfo = faceInfo;

        int tempW,tempH;
        if((faceInfo.getMaxSide()+faceInfo.getLeft()) > width
                || (faceInfo.getMaxSide()+faceInfo.getTop()) > height){
            tempW = faceInfo.getWidth();
            tempH = faceInfo.getHeight();
        }else{
            tempW = faceInfo.getMaxSide();
            tempH = faceInfo.getMaxSide();
        }
        tempW = tempW % 2 ==0 ? tempW : tempW -1;
        tempH = tempH % 2 ==0 ? tempH : tempH -1;
//        facePersion.faceDate = Face.cutNV21(videoFrame.frame, faceInfo.getLeft(), faceInfo.getTop(), tempW, tempH, width, height);
        facePersion.faceDate = ImageUtil.cutNV21(videoFrame.frame, faceInfo.getLeft(), faceInfo.getTop(), tempW, tempH, width, height);
        facePersion.faceDateW = tempW;
        facePersion.faceDateH = tempH;
        facePersion.time = time;
        if(videoFrame.trackMap==null){
            videoFrame.trackMap = new HashMap<>();
        }
        videoFrame.trackMap.put(trackId, faceInfo);
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
            Thread.sleep(80);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 添加到队列B中，并通知队列B相关线程
        synchronized (queueB) {
            queueB.offer(videoFrame);
            queueB.notifyAll();
        }

//        Log.i(TAG, "onPreviewFrame: "+videoFrame.time+" ms");
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
