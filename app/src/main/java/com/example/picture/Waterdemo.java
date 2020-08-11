package com.example.picture;

import android.graphics.Bitmap;
import android.content.res.AssetManager;

public class Waterdemo {
    static {
        System.loadLibrary("native-lib");
    }
    //    public static native int add(int i,int j);
//    public static native String stringFromJNI();
//
//    public native boolean Init(byte[] param, byte[] bin);
    public native boolean Init(AssetManager mgr);

    public native boolean Detect(Bitmap bitmap, int style_type, boolean use_gpu);
}

