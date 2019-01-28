package com.example.l.mobilefacenet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CameraPreviewCallback2 implements PreviewCallback {
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

    private Queue<Face> queueFaceDetect = new ArrayBlockingQueue<>(FACE_DETECT_NUM+1);
    private Queue<Face> queueFaceRecognize = new ArrayBlockingQueue<>(FACE_RECOGNIZE_NUM+1);
//    private Face mFace;

    private Map<Long, FacePersion> map = new HashMap<>();
    private Map<Long, FacePersion> knownMap = new HashMap<>();

    private Queue<VideoFrame> queueA = new LinkedList<>();
    private Queue<VideoFrame> queueB = new LinkedList<>();
    private Queue<VideoFrame> queueC = new LinkedList<>();

    private ExecutorService threadPoolA = Executors.newFixedThreadPool(2);
    private ExecutorService threadPoolB = Executors.newFixedThreadPool(FACE_DETECT_NUM);
    private ExecutorService threadPoolC = Executors.newFixedThreadPool(FACE_RECOGNIZE_NUM);

    private static final Face.ColorType colorType = Face.ColorType.R8G8B8A8;
    private boolean stop = false;
    private AtomicLong track = new AtomicLong(0);

    private List<Persion> persions;

    private AtomicInteger initNum = new AtomicInteger(0);

    public CameraPreviewCallback2(){
        File sdDir = Environment.getExternalStorageDirectory();//get directory
        String sdPath = sdDir.toString() + "/facem/";
        int minFaceSize = CommonUtil.sqrt(CommonUtil.area(width, height) / Face.MIN_FACE_SIZE_SCALE);
        int threadNum = 1;
        for(int i=0; i<FACE_DETECT_NUM+1; i++){
            Face face = new Face();
            face.faceModelInit(sdPath, threadNum, minFaceSize);
            queueFaceDetect.offer(face);
        }
        for(int i=0; i<FACE_RECOGNIZE_NUM+1; i++){
            Face face = new Face();
            face.faceModelInit(sdPath, threadNum, minFaceSize);
            queueFaceRecognize.offer(face);
        }
//        mFace = new Face();
//        mFace.faceModelInit(sdPath, threadNum, minFaceSize);
        startService();
    }

    public void stop(){
        stop = true;
//        mFace.faceModelUnInit();
    }

    public long getTrack(){
        return track.incrementAndGet();
    }

    private void startService(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                initNum.incrementAndGet();
//                while(!stop){
//                    synchronized (queueA){
//                        if(queueA.isEmpty()){
//                            try {
//                                queueA.wait(20);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                    if(!queueA.isEmpty()){
//                        VideoFrame tempVideoFrame = null;
//                        try{
//                            tempVideoFrame = queueA.poll();
//                        }catch (Exception e){
//                            tempVideoFrame = null;
//                        }
//                        final VideoFrame videoFrame = tempVideoFrame ;
//                        if(videoFrame!=null && videoFrame.frame != null && videoFrame.frame.length>0){
//                            threadPoolA.execute(new Runnable() {
//                                @Override
//                                public void run() {
//                                    // 转换视频帧数据 格式、旋转等
//                                    long timeDetectFace = System.currentTimeMillis();
//                                    Bitmap bitmap ;
//                                    int _degrees = cameraActivity.getCameraId() == Camera.CameraInfo.CAMERA_FACING_BACK ? degrees : degrees + 90;
//                                    if(_degrees!=0){
//                                        if(matrix==null){
//                                            matrix = new Matrix();
//                                            matrix.setRotate(_degrees, width/2, height/2);
//                                        }
//                                        bitmap = nv21ToBitmap.nv21ToBitmap(videoFrame.frame, width, height, matrix);
//                                    }else{
//                                        bitmap = nv21ToBitmap.nv21ToBitmap(videoFrame.frame, width, height);
//                                    }
//                                    videoFrame.bitmap = bitmap;
//                                    videoFrame.frame = ImageUtil.getPixelsRGBA(bitmap);
//                                    timeDetectFace = System.currentTimeMillis() - timeDetectFace;
//                                    Log.i(TAG, "nv21ToBitmap time:"+timeDetectFace+"ms");
//                                    // 添加到队列B中，并通知队列B相关线程
//                                    synchronized (queueB) {
//                                        queueB.offer(videoFrame);
//                                        queueB.notifyAll();
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
            }
        }).start();
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
                                    Face mFace = queueFaceDetect.poll();
                                    Face.FaceInfo[] faceInfos = mFace.faceDetect(videoFrame.frame, videoFrame.width, videoFrame.height, colorType);
                                    videoFrame.frame = null;
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
                                                }else{
                                                    videoFrame.frame = null;
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
                                    queueFaceDetect.offer(mFace);
                                }
                            });
                        }
                    }
                }
                int size = queueFaceDetect.size();
                for(int i=0; i<size; i++){
                    Face mFace = queueFaceDetect.poll();
                    mFace.faceModelUnInit();
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
                        if(videoFrame!=null && videoFrame.bitmap != null){
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
                                        Face mFace = queueFaceRecognize.poll();
                                        float[] feature = mFace.faceFeature(facePersion.faceDate, facePersion.faceDateW, facePersion.faceDateH, colorType);
                                        facePersion.faceDate = null;
                                        if(feature==null || feature.length==0){
                                            queueFaceRecognize.offer(mFace);
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
                                            knownMap.put(facePersion.trackId, facePersion);
                                        }
                                        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                                        queueFaceRecognize.offer(mFace);
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
                    Face mFace = queueFaceRecognize.poll();
                    mFace.faceModelUnInit();
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
        Bitmap faceImage = Bitmap.createBitmap(videoFrame.bitmap, faceInfo.getLeft(), faceInfo.getTop(), faceInfo.getWidth(), faceInfo.getHeight());
        byte[] faceDate = ImageUtil.getPixelsRGBA(faceImage);
        long trackId = getTrack();
        FacePersion facePersion = new FacePersion();
        facePersion.trackId = trackId;
        facePersion.faceInfo = faceInfo;
        facePersion.faceDate = faceDate;
        facePersion.faceDateW = faceInfo.getWidth();
        facePersion.faceDateH = faceInfo.getHeight();
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
        if(data==null || data.length<=0 || initNum.get()<4){
            return;
        }
        VideoFrame videoFrame = new VideoFrame();
        videoFrame.time = System.currentTimeMillis();
        videoFrame.frame = Arrays.copyOf(data,data.length);
        videoFrame.width = width;
        videoFrame.height = height;
//        synchronized (queueA) {
//            // 将视频帧添加到队列A中，并通知队列A相关线程
//            queueA.offer(videoFrame);
//            queueA.notifyAll();
//        }

//        // 为了控制帧频，在这里转换帧数据 如果设备性能快，可以放回线程处理
//        // 转换视频帧数据 格式、旋转等
        long timeDetectFace = System.currentTimeMillis();
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
        synchronized (queueB) {
            queueB.offer(videoFrame);
            queueB.notifyAll();
        }
        timeDetectFace = System.currentTimeMillis() - timeDetectFace;
        Log.i(TAG, "nv21ToBitmap time:"+timeDetectFace+"ms");

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
