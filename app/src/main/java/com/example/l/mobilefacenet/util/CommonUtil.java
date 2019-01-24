package com.example.l.mobilefacenet.util;

public class CommonUtil {
    public static int max(int x,int y){
        return x>y? x : y;
    }
    public static long area(int w,int h){
        return 1L * w * h;
    }
    public static int sqrt(double x){
        return (int)Math.ceil(Math.sqrt(x));
    }
}
