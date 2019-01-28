package com.example.l.mobilefacenet.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageUtil {
    /**
     * nv21 裁剪
     * 参考 https://blog.csdn.net/Magic_frank/article/details/70941111
     */
    public static byte[] cutNV21(byte[] srcNv21, int x, int y, int cutW, int cutH, int srcW, int srcH) {

        int i;
        int j = 0;
        int k = 0;

        int len = cutW * cutH * 3 / 2 ;
        byte[] tarNv21 = new byte[len];
        //分配一段内存，用于存储裁剪后的Y分量
        byte[] tmpY = new byte[cutW * cutH];
        //分配一段内存，用于存储裁剪后的UV分量
        byte[] tmpUV = new byte[cutW * cutH / 2];

        for(i=y; i< cutH + y; i++) {
            // 逐行拷贝Y分量，共拷贝cutW*cutH
            System.arraycopy(srcNv21,  x + i * srcW, tmpY, j * cutW, cutW);
            j++;
        }
        for(i=y/2; i< (cutH + y) / 2; i++) {
            //逐行拷贝UV分量，共拷贝cutW*cutH/2
            System.arraycopy(srcNv21,  x + srcW * srcH + i * srcW, tmpUV, k * cutW, cutW);
            k++;
        }
        //将拷贝好的Y，UV分量拷贝到目标内存中
        System.arraycopy(tmpY,  0, tarNv21, 0, cutW * cutH);
        System.arraycopy(tmpUV,  0, tarNv21, cutW * cutH, cutW * cutH / 2);
        tmpY = null;
        tmpUV = null;
        return tarNv21;
    }

    /**
     * Bitmap转化为ARGB数据，再转化为NV21数据
     *
     * @param src    传入的Bitmap，格式为{@link Bitmap.Config#ARGB_8888}
     * @param width  NV21图像的宽度
     * @param height NV21图像的高度
     * @return nv21数据
     */
    public static byte[] bitmapToNv21(Bitmap src, int width, int height) {
        if (src != null && src.getWidth() >= width && src.getHeight() >= height) {
            int[] argb = new int[width * height];
            src.getPixels(argb, 0, width, 0, 0, width, height);
            return argbToNv21(argb, width, height);
        } else {
            return null;
        }
    }

    /**
     * ARGB数据转化为NV21数据
     *
     * @param argb   argb数据
     * @param width  宽度
     * @param height 高度
     * @return nv21数据
     */
    private static byte[] argbToNv21(int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int index = 0;
        byte[] nv21 = new byte[width * height * 3 / 2];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                int R = (argb[index] & 0xFF0000) >> 16;
                int G = (argb[index] & 0x00FF00) >> 8;
                int B = argb[index] & 0x0000FF;
                int Y = (66 * R + 129 * G + 25 * B + 128 >> 8) + 16;
                int U = (-38 * R - 74 * G + 112 * B + 128 >> 8) + 128;
                int V = (112 * R - 94 * G - 18 * B + 128 >> 8) + 128;
                nv21[yIndex++] = (byte) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21.length - 2) {
                    nv21[uvIndex++] = (byte) (V < 0 ? 0 : (V > 255 ? 255 : V));
                    nv21[uvIndex++] = (byte) (U < 0 ? 0 : (U > 255 ? 255 : U));
                }

                ++index;
            }
        }
        return nv21;
    }

    /**
     * bitmap转化为bgr数据，格式为{@link Bitmap.Config#ARGB_8888}
     *
     * @param image 传入的bitmap
     * @return bgr数据
     */
    public static byte[] bitmapToBgr(Bitmap image) {
        if (image == null) {
            return null;
        }
        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        image.copyPixelsToBuffer(buffer);
        byte[] temp = buffer.array();
        byte[] pixels = new byte[(temp.length / 4) * 3];
        for (int i = 0; i < temp.length / 4; i++) {
            pixels[i * 3] = temp[i * 4 + 2];
            pixels[i * 3 + 1] = temp[i * 4 + 1];
            pixels[i * 3 + 2] = temp[i * 4];
        }
        return pixels;
    }

    /**
     * 裁剪bitmap
     *
     * @param bitmap 传入的bitmap
     * @param rect   需要被裁剪的区域
     * @return 被裁剪后的bitmap
     */
    public static Bitmap imageCrop(Bitmap bitmap, Rect rect) {
        if (bitmap == null || rect == null || rect.isEmpty() || bitmap.getWidth() < rect.right || bitmap.getHeight() < rect.bottom) {
            return null;
        }
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), null, false);
    }

    public static Bitmap getBitmapFromUri(Uri uri, Context context) {
        if (uri == null || context == null) {
            return null;
        }
        try {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getRotateBitmap(Bitmap b, float rotateDegree) {
        if (b == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotateDegree);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, false);
    }

    /**
     * 确保传给引擎的BGR24数据宽度为4的倍数
     *
     * @param bitmap 传入的bitmap
     * @return 调整后的bitmap
     */
    public static Bitmap alignBitmapForBgr24(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 4) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        boolean needAdjust = false;
        while (width % 4 != 0) {
            width--;
            needAdjust = true;
        }

        if (needAdjust) {
            bitmap = imageCrop(bitmap, new Rect(0, 0, width, height));
        }
        return bitmap;
    }

    /**
     * 确保传给引擎的NV21数据宽度为4的倍数，高为2的倍数
     *
     * @param bitmap 传入的bitmap
     * @return 调整后的bitmap
     */
    public static Bitmap alignBitmapForNv21(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 4 || bitmap.getHeight() < 2) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        boolean needAdjust = false;
        while (width % 4 != 0) {
            width--;
            needAdjust = true;
        }
        if (height % 2 != 0) {
            height--;
            needAdjust = true;
        }

        if (needAdjust) {
            bitmap = imageCrop(bitmap, new Rect(0, 0, width, height));
        }
        return bitmap;
    }

    /*
     * Refer to https://code.google.com/p/android/issues/detail?id=823
     */
    public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    public static Bitmap rgb2Bitmap(byte[] rgbBytes, int width, int height){
        if(rgbBytes==null || rgbBytes.length==0)return null;
        int[] rgb = new int[rgbBytes.length / 3];
        for(int i=0; i<rgb.length; i++){
            int index = i * 3;
            rgb[i] = 0xFF000000 | rgbBytes[index+0] << 16 | rgbBytes[index+1] << 8  | rgbBytes[index+2] ;
        }
        return Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
    }

    public static class NV21ToBitmap {
        private RenderScript rs;
        private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
        private Type.Builder yuvType, rgbaType;
        private Allocation in, out;

        public NV21ToBitmap(Context context) {
            rs = RenderScript.create(context);
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        }

        public byte[] nv21ToByte(byte[] nv21, int width, int height) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }

            in.copyFrom(nv21);

            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);

            byte[] byteout = new byte[4*width*height];
            out.copyTo(byteout);

            return byteout;
        }

        public Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }

            in.copyFrom(nv21);

            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);

            Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            out.copyTo(bmpout);

            return bmpout;

        }

        public Bitmap nv21ToBitmap(byte[] nv21, int width, int height, Matrix matrix) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }

            in.copyFrom(nv21);

            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);

            Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            out.copyTo(bmpout);
            return Bitmap.createBitmap(bmpout,0,0,width, height,matrix,true);

        }
    }

    public static Bitmap decodeUri(ContextWrapper contextWrapper,Uri selectedImage) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(contextWrapper.getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(contextWrapper.getContentResolver().openInputStream(selectedImage), null, o2);
    }

    //get pixels
    public static byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }
}
