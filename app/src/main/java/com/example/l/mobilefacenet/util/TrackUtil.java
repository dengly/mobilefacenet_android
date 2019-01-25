package com.example.l.mobilefacenet.util;

import android.graphics.Rect;

import com.example.l.mobilefacenet.Face;

/**
 * 人脸跟踪工具
 */
public class TrackUtil {
    public static final float SIMILARITY_RECT = 0.3f;

    /**
     * 粗暴处理，同一张人脸在两帧之间的位置变化不大，且人脸面积变化也不大。即两帧之间同一个区域基本上是同一张人脸。
     * 前提条件是帧频要高，处理速度要快。
     * @param fSimilarity
     * @param rect1
     * @param rect2
     * @return
     */
    public static boolean isSameFace(float fSimilarity, Rect rect1, Rect rect2) {
        int left = Math.max(rect1.left, rect2.left);
        int top = Math.max(rect1.top, rect2.top);
        int right = Math.min(rect1.right, rect2.right);
        int bottom = Math.min(rect1.bottom, rect2.bottom);

        int innerArea = (right - left) * (bottom - top);

        return left < right
                && top < bottom
                && rect2.width() * rect2.height() * fSimilarity <= innerArea
                && rect1.width() * rect1.height() * fSimilarity <= innerArea;
    }
    public static boolean isSameFace(float fSimilarity, Face.FaceInfo faceInfo1, Face.FaceInfo faceInfo2) {
        int left = Math.max(faceInfo1.getLeft(), faceInfo2.getLeft());
        int top = Math.max(faceInfo1.getTop(), faceInfo2.getTop());
        int right = Math.min(faceInfo1.getRight(), faceInfo2.getRight());
        int bottom = Math.min(faceInfo1.getBottom(), faceInfo2.getBottom());

        int innerArea = (right - left) * (bottom - top);

        return left < right
                && top < bottom
                && faceInfo2.getWidth() * faceInfo2.getHeight() * fSimilarity <= innerArea
                && faceInfo1.getWidth() * faceInfo1.getHeight() * fSimilarity <= innerArea;
    }
}
