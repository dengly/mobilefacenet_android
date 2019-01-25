package com.example.l.mobilefacenet.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class ImageUtil {
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
